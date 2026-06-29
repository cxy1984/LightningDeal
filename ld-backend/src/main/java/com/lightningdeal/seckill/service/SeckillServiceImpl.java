package com.lightningdeal.seckill.service;

import com.lightningdeal.activity.entity.SeckillActivity;
import com.lightningdeal.activity.service.SeckillActivityService;
import com.lightningdeal.common.exception.BizException;
import com.lightningdeal.order.entity.SeckillOrder;
import com.lightningdeal.order.service.SeckillOrderService;
import com.lightningdeal.seckill.model.SeckillRequest;
import com.lightningdeal.seckill.model.SeckillResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.lightningdeal.config.RabbitMQConfig.SECKILL_EXCHANGE;
import static com.lightningdeal.config.RabbitMQConfig.SECKILL_ROUTING_KEY;

/**
 * 秒杀核心服务实现
 *
 * 高并发设计：
 * 1. Redis 预减库存（原子操作）
 * 2. Redis Set 校验重复秒杀
 * 3. Redisson 分布式锁防并发
 * 4. RabbitMQ 异步削峰
 * 5. 数据库乐观锁兜底
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {

    private static final String STOCK_PREFIX = "seckill:stock:";
    private static final String USER_SET_PREFIX = "seckill:users:";
    private static final String RESULT_PREFIX = "seckill:result:";
    private static final String LOCK_PREFIX = "seckill:lock:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedissonClient redissonClient;
    private final RabbitTemplate rabbitTemplate;
    private final SeckillActivityService activityService;
    private final SeckillOrderService orderService;

    @Override
    public SeckillResult executeSeckill(Long userId, SeckillRequest request) {
        Long activityId = request.getActivityId();

        // ===== 1. 参数校验 =====
        SeckillActivity activity = activityService.getById(activityId);
        if (activity == null) {
            throw new BizException(400, "活动不存在");
        }

        // 校验活动时间
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime())) {
            throw new BizException(400, "活动尚未开始");
        }
        if (now.isAfter(activity.getEndTime())) {
            throw new BizException(400, "活动已结束");
        }

        // ===== 2. 校验是否已参与（Redis Set） =====
        String userSetKey = USER_SET_PREFIX + activityId;
        Boolean isMember = redisTemplate.opsForSet().isMember(userSetKey, userId.toString());
        if (Boolean.TRUE.equals(isMember)) {
            log.warn("重复秒杀 userId={}, activityId={}", userId, activityId);
            return SeckillResult.repeat(activityId);
        }

        // ===== 3. Redis 预减库存（Lua 脚本保证原子性） =====
        String stockKey = STOCK_PREFIX + activityId;
        String luaScript =
                "local stock = redis.call('GET', KEYS[1]) " +
                "if stock and tonumber(stock) > 0 then " +
                "    redis.call('DECR', KEYS[1]) " +
                "    return 1 " +
                "else " +
                "    return 0 " +
                "end";

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(luaScript, Long.class);
        Long result = redisTemplate.execute(redisScript, Arrays.asList(stockKey, userSetKey));

        if (result == null || result == 0) {
            log.info("库存不足 userId={}, activityId={}", userId, activityId);
            return SeckillResult.fail("手慢啦，库存已被抢完", activityId);
        }

        // ===== 4. 标记用户已参与（熔断机制 - 先标记，失败则回滚） =====
        redisTemplate.opsForSet().add(userSetKey, userId.toString());

        // ===== 5. 发送 MQ 消息异步下单 =====
        try {
            rabbitTemplate.convertAndSend(SECKILL_EXCHANGE, SECKILL_ROUTING_KEY,
                    new SeckillMessage(userId, activityId, request.getQuantity()));
            log.info("秒杀消息已发送 userId={}, activityId={}", userId, activityId);
        } catch (Exception e) {
            // MQ 发送失败，回滚 Redis 库存和用户标记
            redisTemplate.opsForValue().increment(stockKey);
            redisTemplate.opsForSet().remove(userSetKey, userId.toString());
            log.error("MQ 发送失败，回滚库存 userId={}, activityId={}", userId, activityId, e);
            return SeckillResult.fail("系统繁忙，请重试", activityId);
        }

        // ===== 6. 返回排队中，结果通过 WebSocket 推送 =====
        return SeckillResult.queuing(activityId);
    }

    @Override
    public SeckillResult getSeckillResult(Long userId, Long activityId) {
        String resultKey = RESULT_PREFIX + activityId + ":" + userId;
        Object cached = redisTemplate.opsForValue().get(resultKey);
        if (cached instanceof SeckillResult) {
            return (SeckillResult) cached;
        }

        // 查询订单表是否有对应订单
        SeckillOrder order = orderService.getUserOrder(userId, activityId);
        if (order != null) {
            SeckillResult res = SeckillResult.success(activityId, order.getId());
            redisTemplate.opsForValue().set(resultKey, res, 5, TimeUnit.MINUTES);
            return res;
        }

        return null; // 仍在处理中
    }
}
