package com.lightningdeal.activity.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lightningdeal.activity.entity.SeckillActivity;
import com.lightningdeal.activity.mapper.SeckillActivityMapper;
import com.lightningdeal.activity.model.ActivityVO;
import com.lightningdeal.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
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

    @Override
    public IPage<ActivityVO> listActivities(int page, int size, Integer status) {
        LambdaQueryWrapper<SeckillActivity> wrapper = new LambdaQueryWrapper<SeckillActivity>()
                .eq(status != null && status > 0, SeckillActivity::getStatus, status)
                .orderByDesc(SeckillActivity::getCreateTime);

        IPage<SeckillActivity> entityPage = page(new Page<>(page, size), wrapper);
        return entityPage.convert(this::toVO);
    }

    @Override
    public ActivityVO getActivityDetail(Long activityId) {
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
        return toVO(getById(activity.getId()));
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
    public void incrRedisStock(Long activityId) {
        String stockKey = STOCK_PREFIX + activityId;
        redisTemplate.opsForValue().increment(stockKey);
        redisTemplate.opsForValue().decrement(SOLD_PREFIX + activityId);
    }

    private ActivityVO toVO(SeckillActivity activity) {
        ActivityVO vo = new ActivityVO();
        BeanUtils.copyProperties(activity, vo);

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
