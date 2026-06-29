package com.lightningdeal.seckill.service;

import com.lightningdeal.activity.service.SeckillActivityService;
import com.lightningdeal.common.exception.BizException;
import com.lightningdeal.order.entity.SeckillOrder;
import com.lightningdeal.order.service.SeckillOrderService;
import com.lightningdeal.seckill.model.SeckillResult;
import com.lightningdeal.websocket.SeckillResultWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

import static com.lightningdeal.config.RabbitMQConfig.*;

/**
 * 秒杀订单 MQ 消费者
 * 异步处理下单，削峰填谷
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillOrderConsumer {

    private static final String RESULT_PREFIX = "seckill:result:";

    private final SeckillOrderService orderService;
    private final SeckillActivityService activityService;
    private final RabbitTemplate rabbitTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SeckillResultWebSocketHandler webSocketHandler;

    /**
     * 消费秒杀订单消息
     */
    @RabbitListener(queues = SECKILL_QUEUE)
    @Transactional(rollbackFor = Exception.class)
    public void handleSeckillOrder(SeckillMessage message) {
        Long userId = message.getUserId();
        Long activityId = message.getActivityId();
        Integer quantity = message.getQuantity();

        log.info("消费秒杀消息 userId={}, activityId={}", userId, activityId);

        try {
            // ===== 1. 数据库乐观锁扣减库存 =====
            boolean success = activityService.decrementDbStock(activityId);
            if (!success) {
                // 数据库库存不足（兜底），回滚 Redis 库存
                activityService.incrRedisStock(activityId);
                SeckillResult result = SeckillResult.fail("库存不足", activityId);
                notifyUser(userId, activityId, result);
                return;
            }

            // ===== 2. 创建订单 =====
            SeckillOrder order = orderService.createOrder(userId, activityId, quantity);

            // ===== 3. 推送成功结果 =====
            SeckillResult result = SeckillResult.success(activityId, order.getId());
            notifyUser(userId, activityId, result);

            // ===== 4. 发送延迟消息，超时未支付则取消订单 =====
            rabbitTemplate.convertAndSend(DELAY_EXCHANGE, DELAY_ROUTING_KEY,
                    order.getId(), msg -> {
                        msg.getMessageProperties().setDelay(30 * 60 * 1000); // 30min 延迟
                        return msg;
                    });

        } catch (Exception e) {
            log.error("订单处理异常 userId={}, activityId={}", userId, activityId, e);
            // 回滚 Redis 库存
            activityService.incrRedisStock(activityId);
            SeckillResult result = SeckillResult.fail("系统繁忙", activityId);
            notifyUser(userId, activityId, result);
        }
    }

    /**
     * 消费死信队列（重试耗尽的消息）
     */
    @RabbitListener(queues = DEAD_QUEUE)
    public void handleDeadLetter(SeckillMessage message) {
        log.error("死信消息，人工处理 userId={}, activityId={}", message.getUserId(), message.getActivityId());
        // 回滚 Redis 库存
        activityService.incrRedisStock(message.getActivityId());
        SeckillResult result = SeckillResult.fail("超时未处理", message.getActivityId());
        notifyUser(message.getUserId(), message.getActivityId(), result);
    }

    /**
     * 消费延迟消息，超时取消订单
     */
    @RabbitListener(queues = DELAY_QUEUE)
    @Transactional(rollbackFor = Exception.class)
    public void handleOrderTimeout(Long orderId) {
        log.info("订单超时取消 orderId={}", orderId);
        SeckillOrder order = orderService.getById(orderId);
        if (order != null && order.getStatus() == 0) { // 未支付
            orderService.cancelOrder(orderId);
            activityService.incrRedisStock(order.getActivityId());
            // 移除用户标记，允许重新抢购
            redisTemplate.opsForSet().remove("seckill:users:" + order.getActivityId(), order.getUserId().toString());
            SeckillResult result = SeckillResult.fail("订单超时已取消", order.getActivityId());
            notifyUser(order.getUserId(), order.getActivityId(), result);
        }
    }

    /**
     * 推送结果到 WebSocket 并缓存到 Redis
     */
    private void notifyUser(Long userId, Long activityId, SeckillResult result) {
        // WebSocket 推送
        webSocketHandler.sendResult(userId, result);
        // Redis 缓存结果（供轮询查询）
        redisTemplate.opsForValue().set(RESULT_PREFIX + activityId + ":" + userId, result, 5, TimeUnit.MINUTES);
    }
}
