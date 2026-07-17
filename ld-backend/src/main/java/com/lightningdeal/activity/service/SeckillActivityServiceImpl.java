package com.lightningdeal.activity.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lightningdeal.activity.entity.SeckillActivity;
import com.lightningdeal.activity.mapper.SeckillActivityMapper;
import com.lightningdeal.activity.model.ActivityVO;
import com.lightningdeal.common.exception.BizException;
import com.lightningdeal.common.service.BloomFilterService;
import com.lightningdeal.order.entity.SeckillOrder;
import com.lightningdeal.order.service.SeckillOrderService;
import com.lightningdeal.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 秒杀活动服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillActivityServiceImpl extends ServiceImpl<SeckillActivityMapper, SeckillActivity>
        implements SeckillActivityService {

    private static final String STOCK_PREFIX = "seckill:stock:";
    private static final String SOLD_PREFIX = "seckill:sold:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final SeckillActivityMapper seckillActivityMapper;
    private final SearchService searchService;
    private final BloomFilterService bloomFilterService;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private SeckillOrderService orderService;

    @Override
    public IPage<ActivityVO> listActivities(int page, int size, Integer status, String name) {
        LambdaQueryWrapper<SeckillActivity> wrapper = new LambdaQueryWrapper<SeckillActivity>()
                .eq(status != null && status > 0, SeckillActivity::getStatus, status)
                .like(name != null && !name.isEmpty(), SeckillActivity::getName, name)
                .orderByDesc(SeckillActivity::getCreateTime);

        IPage<SeckillActivity> entityPage = page(new Page<>(page, size), wrapper);
        return entityPage.convert(this::toVO);
    }

    @Override
    public ActivityVO getActivityDetail(Long activityId) {
        // 布隆过滤器拦截不存在的活动 ID
        if (!bloomFilterService.mightContain(activityId)) {
            throw new BizException(404, "活动不存在");
        }
        SeckillActivity activity = getById(activityId);
        if (activity == null) {
            throw new BizException(404, "活动不存在");
        }
        return toVO(activity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActivityVO createActivity(SeckillActivity activity) {
        activity.setSoldStock(0);
        activity.setStatus(0); // 草稿
        save(activity);
        log.info("创建秒杀活动 activityId={}, name={}", activity.getId(), activity.getName());
        searchService.syncActivity(getById(activity.getId()));
        return toVO(activity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActivityVO updateActivity(SeckillActivity activity) {
        SeckillActivity exist = getById(activity.getId());
        if (exist == null) {
            throw new BizException(404, "活动不存在");
        }
        // 如果活动已开始，不允许修改关键字段
        if (exist.getStatus() >= 2) {
            throw new BizException(400, "活动已开始，无法修改");
        }
        updateById(activity);

        // 如果修改了库存，重新预热
        if (activity.getTotalStock() != null) {
            preheatStock(activity.getId());
        }
        searchService.syncActivity(getById(activity.getId()));
        return toVO(getById(activity.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteActivity(Long activityId) {
        SeckillActivity activity = getById(activityId);
        if (activity == null) {
            throw new BizException(404, "活动不存在");
        }
        // 判断是否实际处于进行中或已结束（数据库 status>=2 或当前时间超过开始时间）
        boolean isRunning = activity.getStatus() >= 2
                || (activity.getStatus() >= 1 && LocalDateTime.now().isAfter(activity.getStartTime()));
        if (isRunning) {
            throw new BizException(400, "活动已开始或结束，无法删除");
        }

        // 1. 清理 Redis 库存和标记（先于事务执行，避免事务内 Redis 操作异常）
        clearActivityRedisCache(activityId);

        // 2. 作废该活动所有未支付的订单
        List<SeckillOrder> pendingOrders = orderService.lambdaQuery()
                .eq(SeckillOrder::getActivityId, activityId)
                .eq(SeckillOrder::getStatus, 0)
                .list();
        for (SeckillOrder order : pendingOrders) {
            order.setStatus(2); // 已取消
            orderService.updateById(order);
        }

        // 3. 逻辑删除活动
        removeById(activityId);

        // 5. 同步删除 ES 索引
        searchService.deleteIndex(activityId);
        // 注：布隆过滤器不支持删除元素，已删除的活动 ID 仍会通过布隆过滤器，
        // 但后续的 DB 查询会返回 null（逻辑删除），误判不影响正确性

        log.info("删除活动 activityId={}, name={}, 取消订单数={}",
                activityId, activity.getName(), pendingOrders.size());
    }

    /**
     * 清理活动在 Redis 中的缓存
     */
    private void clearActivityRedisCache(Long activityId) {
        String stockKey = STOCK_PREFIX + activityId;
        String soldKey = SOLD_PREFIX + activityId;
        String usersKey = "seckill:users:" + activityId;

        redisTemplate.delete(stockKey);
        redisTemplate.delete(soldKey);
        redisTemplate.delete(usersKey);
        // 使用 SCAN 删除该活动的结果缓存（避免 KEYS 阻塞）
        try {
            redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
                org.springframework.data.redis.core.ScanOptions options = org.springframework.data.redis.core.ScanOptions.scanOptions()
                        .match("seckill:result:" + activityId + ":*").count(100).build();
                org.springframework.data.redis.core.Cursor<byte[]> cursor = connection.scan(options);
                while (cursor.hasNext()) {
                    connection.del(cursor.next());
                }
                cursor.close();
                return null;
            });
        } catch (Exception e) {
            log.warn("SCAN 删除 result 缓存失败 activityId={}", activityId, e);
        }
        log.debug("清除活动 Redis 缓存 activityId={}", activityId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActivityVO updateStatus(Long activityId, Integer status) {
        SeckillActivity activity = getById(activityId);
        if (activity == null) {
            throw new BizException(404, "活动不存在");
        }
        if (status < 0 || status > 3) {
            throw new BizException(400, "无效的状态值");
        }
        if (activity.getStatus() >= 2 && status < 2) {
            throw new BizException(400, "活动已开始或结束，无法回退状态");
        }
        activity.setStatus(status);
        updateById(activity);
        log.info("更新活动状态 activityId={}, status={}", activityId, status);

        // 同步 ES
        searchService.syncActivity(getById(activityId));

        // 上架时预热库存
        if (status == 1) {
            preheatStock(activityId);
        }
        return toVO(getById(activityId));
    }

    @Override
    public void preheatStock(Long activityId) {
        SeckillActivity activity = getById(activityId);
        if (activity == null) {
            throw new BizException(404, "活动不存在");
        }

        String stockKey = STOCK_PREFIX + activityId;
        // 剩余库存 = 总库存 - 已售
        int remain = activity.getTotalStock() - activity.getSoldStock();

        redisTemplate.opsForValue().set(stockKey, remain);
        redisTemplate.opsForValue().set(SOLD_PREFIX + activityId, activity.getSoldStock());

        log.info("库存预热 activityId={}, total={}, sold={}, remain={}",
                activityId, activity.getTotalStock(), activity.getSoldStock(), remain);
    }

    @Override
    public boolean decrementDbStock(Long activityId) {
        return seckillActivityMapper.decrementStock(activityId) > 0;
    }

    /**
     * 获取 Redis 中剩余库存
     */
    public Integer getRedisStock(Long activityId) {
        Object stock = redisTemplate.opsForValue().get(STOCK_PREFIX + activityId);
        return stock == null ? null : (Integer) stock;
    }

    /**
     * Redis 原子扣减库存，返回扣减后的剩余库存
     */
    public Long decrRedisStock(Long activityId) {
        String stockKey = STOCK_PREFIX + activityId;
        Long remain = redisTemplate.opsForValue().decrement(stockKey);
        if (remain != null && remain >= 0) {
            redisTemplate.opsForValue().increment(SOLD_PREFIX + activityId);
        }
        return remain;
    }

    /**
     * Redis 回补库存（下单失败或取消订单时使用）
     */
    @Override
    public void revertSoldStock(Long activityId) {
        seckillActivityMapper.revertSoldStock(activityId);
        log.info("回退已售数量 activityId={}", activityId);
    }

    @Override
    public void incrRedisStock(Long activityId) {
        String stockKey = STOCK_PREFIX + activityId;
        redisTemplate.opsForValue().increment(stockKey);
        redisTemplate.opsForValue().decrement(SOLD_PREFIX + activityId);
    }

    private ActivityVO toVO(SeckillActivity activity) {
        ActivityVO vo = new ActivityVO();
        BeanUtils.copyProperties(activity, vo);

        // 保留数据库原始状态（管理后台用）
        vo.setDbStatus(activity.getStatus());

        // 计算剩余库存（优先使用 Redis 中的实时库存）
        Integer redisStock = getRedisStock(activity.getId());
        if (redisStock != null) {
            vo.setRemainStock(redisStock);
        } else {
            vo.setRemainStock(activity.getTotalStock() - activity.getSoldStock());
        }

        // 计算倒计时
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime())) {
            vo.setCountingDown(true);
            vo.setCountdownMillis(Duration.between(now, activity.getStartTime()).toMillis());
            vo.setStatus(1); // 上架等待
        } else if (now.isAfter(activity.getEndTime())) {
            vo.setCountingDown(false);
            vo.setCountdownMillis(0);
            vo.setStatus(3); // 已结束
        } else {
            vo.setCountingDown(false);
            vo.setCountdownMillis(0);
            vo.setStatus(2); // 进行中
        }

        return vo;
    }
}
