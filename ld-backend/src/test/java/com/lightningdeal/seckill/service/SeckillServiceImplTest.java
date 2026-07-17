package com.lightningdeal.seckill.service;

import com.lightningdeal.activity.entity.SeckillActivity;
import com.lightningdeal.activity.service.SeckillActivityService;
import com.lightningdeal.common.exception.BizException;
import com.lightningdeal.common.service.BloomFilterService;
import com.lightningdeal.dashboard.service.DashboardService;
import com.lightningdeal.order.service.SeckillOrderService;
import com.lightningdeal.seckill.model.SeckillRequest;
import com.lightningdeal.seckill.model.SeckillResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static com.lightningdeal.config.RabbitMQConfig.SECKILL_EXCHANGE;
import static com.lightningdeal.config.RabbitMQConfig.SECKILL_ROUTING_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SeckillServiceImpl 单元测试
 *
 * 测试目标：覆盖 executeSeckill() 全部 10+ 个分支
 * - 正常流程：库存够 + MQ 可用 → 返回排队中
 * - 边界条件：活动不存在/未开始/已结束 → 抛异常
 * - 限购拦截：Redis 超限 / DB 超限 → 返回 fail
 * - 库存不足：Lua 脚本返回 0 → 返回 fail
 * - MQ 异常：发送失败 → 回滚库存+用户标记 → 返回 fail
 * - 降级流程：MQ 不可用 → 走同步下单 → DB 扣库存
 */
@ExtendWith(MockitoExtension.class)
class SeckillServiceImplTest {

    private static final Long USER_ID = 42L;
    private static final Long ACTIVITY_ID = 1L;

    @Mock RedisTemplate<String, Object> redisTemplate;
    @Mock RedissonClient redissonClient;
    @Mock SeckillActivityService activityService;
    @Mock SeckillOrderService orderService;
    @Mock DashboardService dashboardService;
    @Mock BloomFilterService bloomFilterService;
    @Mock ObjectProvider<RabbitTemplate> rabbitTemplateProvider;

    @Mock ValueOperations<String, Object> valueOps;
    @Mock SetOperations<String, Object> setOps;

    @InjectMocks
    SeckillServiceImpl seckillService;

    @Captor
    ArgumentCaptor<List<String>> redisKeyCaptor;

    private SeckillRequest request;
    private SeckillActivity activity;

    @BeforeEach
    void setUp() {
        request = new SeckillRequest();
        request.setActivityId(ACTIVITY_ID);
        request.setQuantity(1);

        activity = new SeckillActivity();
        activity.setId(ACTIVITY_ID);
        activity.setGoodsName("测试商品");
        activity.setLimitPerUser(3);
        activity.setStartTime(LocalDateTime.now().minusMinutes(30));
        activity.setEndTime(LocalDateTime.now().plusMinutes(30));

        // redisTemplate.opsForValue() 和 opsForSet() 默认为 mock
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOps);
        // 布隆过滤器默认通过（mightContain 返回 true）
        lenient().when(bloomFilterService.mightContain(anyLong())).thenReturn(true);
    }

    // ==================== 正常流程 ====================

    @Test
    void shouldReturnQueuingWhenSeckillSuccess() {
        // 活动存在，时间有效
        when(activityService.getById(ACTIVITY_ID)).thenReturn(activity);
        // 限购未超（Redis 计数为 0）
        when(valueOps.get(anyString())).thenReturn(0);
        // DB 限购检查通过
        when(orderService.countUserOrders(USER_ID, ACTIVITY_ID)).thenReturn(0L);
        // Lua 脚本：库存足够
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any()))
                .thenReturn(1L);
        // MQ 可用
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        when(rabbitTemplateProvider.getIfAvailable()).thenReturn(rabbitTemplate);

        // 执行
        SeckillResult result = seckillService.executeSeckill(USER_ID, request);

        // 验证：返回排队中
        assertThat(result.getStatus()).isEqualTo(SeckillResult.STATUS_QUEUING);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).isEqualTo("排队中");
        assertThat(result.getActivityId()).isEqualTo(ACTIVITY_ID);

        // 验证 MQ 消息已发送
        verify(rabbitTemplate).convertAndSend(
                eq(SECKILL_EXCHANGE), eq(SECKILL_ROUTING_KEY), any(Object.class));

        // 验证用户标记已写入 Redis Set
        verify(setOps).add("seckill:users:" + ACTIVITY_ID, USER_ID.toString());
    }

    // ==================== 参数校验失败 ====================

    @Test
    void shouldThrowExceptionWhenActivityNotExist() {
        when(activityService.getById(ACTIVITY_ID)).thenReturn(null);

        assertThatThrownBy(() -> seckillService.executeSeckill(USER_ID, request))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("活动不存在");
    }

    @Test
    void shouldThrowExceptionWhenActivityNotStarted() {
        activity.setStartTime(LocalDateTime.now().plusMinutes(10));
        when(activityService.getById(ACTIVITY_ID)).thenReturn(activity);

        assertThatThrownBy(() -> seckillService.executeSeckill(USER_ID, request))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("尚未开始");
    }

    @Test
    void shouldThrowExceptionWhenActivityEnded() {
        activity.setEndTime(LocalDateTime.now().minusMinutes(1));
        when(activityService.getById(ACTIVITY_ID)).thenReturn(activity);

        assertThatThrownBy(() -> seckillService.executeSeckill(USER_ID, request))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已结束");
    }

    // ==================== 限购拦截 ====================

    @Test
    void shouldReturnFailWhenRedisLimitExceeded() {
        // Redis 计数 >= limitPerUser
        when(activityService.getById(ACTIVITY_ID)).thenReturn(activity);
        when(valueOps.get("seckill:user_count:" + ACTIVITY_ID + ":" + USER_ID))
                .thenReturn(3); // limitPerUser = 3

        SeckillResult result = seckillService.executeSeckill(USER_ID, request);

        assertThat(result.getStatus()).isEqualTo(SeckillResult.STATUS_FAIL);
        assertThat(result.getMessage()).contains("已达到限购数量");
    }

    @Test
    void shouldReturnFailWhenRedisLimitExceededWithLongType() {
        // Redis 存的是 Long 类型（INCR 返回的类型）
        when(activityService.getById(ACTIVITY_ID)).thenReturn(activity);
        when(valueOps.get("seckill:user_count:" + ACTIVITY_ID + ":" + USER_ID))
                .thenReturn(3L); // Long 类型

        SeckillResult result = seckillService.executeSeckill(USER_ID, request);

        assertThat(result.getStatus()).isEqualTo(SeckillResult.STATUS_FAIL);
        assertThat(result.getMessage()).contains("已达到限购数量");
    }

    @Test
    void shouldReturnFailWhenDbLimitExceeded() {
        when(activityService.getById(ACTIVITY_ID)).thenReturn(activity);
        // Redis 计数为 0（通过）
        when(valueOps.get("seckill:user_count:" + ACTIVITY_ID + ":" + USER_ID))
                .thenReturn(0);
        // DB 查询超限
        when(orderService.countUserOrders(USER_ID, ACTIVITY_ID)).thenReturn(3L);

        SeckillResult result = seckillService.executeSeckill(USER_ID, request);

        assertThat(result.getStatus()).isEqualTo(SeckillResult.STATUS_FAIL);
        assertThat(result.getMessage()).contains("已达到限购数量");
    }

    // ==================== 库存不足 ====================

    @Test
    void shouldReturnFailWhenStockNotEnough() {
        when(activityService.getById(ACTIVITY_ID)).thenReturn(activity);
        when(valueOps.get("seckill:user_count:" + ACTIVITY_ID + ":" + USER_ID))
                .thenReturn(0);
        when(orderService.countUserOrders(USER_ID, ACTIVITY_ID)).thenReturn(0L);
        // Lua 脚本返回 0 → 库存不足
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any()))
                .thenReturn(0L);

        SeckillResult result = seckillService.executeSeckill(USER_ID, request);

        assertThat(result.getStatus()).isEqualTo(SeckillResult.STATUS_FAIL);
        assertThat(result.getMessage()).contains("库存已被抢完");
    }

    // ==================== Lua 脚本 key 验证 ====================

    @Test
    void shouldPassCorrectKeysToLuaScript() {
        when(activityService.getById(ACTIVITY_ID)).thenReturn(activity);
        when(valueOps.get("seckill:user_count:" + ACTIVITY_ID + ":" + USER_ID))
                .thenReturn(0);
        when(orderService.countUserOrders(USER_ID, ACTIVITY_ID)).thenReturn(0L);
        when(redisTemplate.execute(any(DefaultRedisScript.class), redisKeyCaptor.capture(), any()))
                .thenReturn(1L);
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        when(rabbitTemplateProvider.getIfAvailable()).thenReturn(rabbitTemplate);

        seckillService.executeSeckill(USER_ID, request);

        List<String> keys = redisKeyCaptor.getValue();
        // Lua 脚本接收 2 个 key：stockKey 和 userCountKey
        assertThat(keys).hasSize(2);
        assertThat(keys.get(0)).isEqualTo("seckill:stock:" + ACTIVITY_ID);
        assertThat(keys.get(1)).isEqualTo("seckill:user_count:" + ACTIVITY_ID + ":" + USER_ID);
    }

    // ==================== MQ 发送失败回滚 ====================

    @Test
    void shouldRollbackWhenMqSendFails() {
        when(activityService.getById(ACTIVITY_ID)).thenReturn(activity);
        when(valueOps.get("seckill:user_count:" + ACTIVITY_ID + ":" + USER_ID))
                .thenReturn(0);
        when(orderService.countUserOrders(USER_ID, ACTIVITY_ID)).thenReturn(0L);
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any()))
                .thenReturn(1L);

        // MQ 可用但发送时抛异常
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        when(rabbitTemplateProvider.getIfAvailable()).thenReturn(rabbitTemplate);
        doThrow(new RuntimeException("MQ 连接失败"))
                .when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(Object.class));

        SeckillResult result = seckillService.executeSeckill(USER_ID, request);

        // 验证：返回失败
        assertThat(result.getStatus()).isEqualTo(SeckillResult.STATUS_FAIL);
        assertThat(result.getMessage()).contains("系统繁忙");

        // 验证 Redis 回滚——库存 +1
        verify(valueOps).increment("seckill:stock:" + ACTIVITY_ID);
        // 验证 Redis 回滚——移除用户标记
        verify(setOps).remove("seckill:users:" + ACTIVITY_ID, USER_ID.toString());
        // 验证 Redis 回滚——限购计数 -1
        verify(valueOps).decrement("seckill:user_count:" + ACTIVITY_ID + ":" + USER_ID);
    }

    // ==================== MQ 不可用走同步下单 ====================

    @Test
    void shouldFallbackToSyncOrderWhenMqUnavailable() {
        when(activityService.getById(ACTIVITY_ID)).thenReturn(activity);
        when(valueOps.get("seckill:user_count:" + ACTIVITY_ID + ":" + USER_ID))
                .thenReturn(0);
        when(orderService.countUserOrders(USER_ID, ACTIVITY_ID)).thenReturn(0L);
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any()))
                .thenReturn(1L);
        // MQ 不可用（getIfAvailable 返回 null）
        when(rabbitTemplateProvider.getIfAvailable()).thenReturn(null);
        // DB 扣库存成功
        when(activityService.decrementDbStock(ACTIVITY_ID)).thenReturn(true);
        // 订单创建成功
        com.lightningdeal.order.entity.SeckillOrder mockOrder =
                new com.lightningdeal.order.entity.SeckillOrder();
        mockOrder.setId(999L);
        when(orderService.createOrder(USER_ID, ACTIVITY_ID, 1)).thenReturn(mockOrder);

        SeckillResult result = seckillService.executeSeckill(USER_ID, request);

        // 同步下单走完应该返回排队中（与 MQ 模式一致的返回）
        assertThat(result.getStatus()).isEqualTo(SeckillResult.STATUS_QUEUING);
        verify(activityService).decrementDbStock(ACTIVITY_ID);
        verify(orderService).createOrder(USER_ID, ACTIVITY_ID, 1);
    }

    @Test
    void shouldRollbackWhenSyncDbStockFails() {
        when(activityService.getById(ACTIVITY_ID)).thenReturn(activity);
        when(valueOps.get("seckill:user_count:" + ACTIVITY_ID + ":" + USER_ID))
                .thenReturn(0);
        when(orderService.countUserOrders(USER_ID, ACTIVITY_ID)).thenReturn(0L);
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any()))
                .thenReturn(1L);
        when(rabbitTemplateProvider.getIfAvailable()).thenReturn(null);
        // DB 扣库存失败（乐观锁冲突）
        when(activityService.decrementDbStock(ACTIVITY_ID)).thenReturn(false);

        SeckillResult result = seckillService.executeSeckill(USER_ID, request);

        assertThat(result.getStatus()).isEqualTo(SeckillResult.STATUS_FAIL);
        assertThat(result.getMessage()).isEqualTo("库存不足");
        // 验证回滚了 Redis 库存
        verify(activityService).incrRedisStock(ACTIVITY_ID);
    }

    @Test
    void shouldRollbackWhenSyncOrderCreationFails() {
        when(activityService.getById(ACTIVITY_ID)).thenReturn(activity);
        when(valueOps.get("seckill:user_count:" + ACTIVITY_ID + ":" + USER_ID))
                .thenReturn(0);
        when(orderService.countUserOrders(USER_ID, ACTIVITY_ID)).thenReturn(0L);
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any()))
                .thenReturn(1L);
        when(rabbitTemplateProvider.getIfAvailable()).thenReturn(null);
        when(activityService.decrementDbStock(ACTIVITY_ID)).thenReturn(true);
        // 订单创建抛异常
        when(orderService.createOrder(USER_ID, ACTIVITY_ID, 1))
                .thenThrow(new RuntimeException("DB 写入失败"));

        SeckillResult result = seckillService.executeSeckill(USER_ID, request);

        assertThat(result.getStatus()).isEqualTo(SeckillResult.STATUS_FAIL);
        assertThat(result.getMessage()).isEqualTo("下单失败");
        verify(activityService).incrRedisStock(ACTIVITY_ID);
    }

    // ==================== getSeckillResult ====================

    @Test
    void shouldReturnCachedResultWhenExists() {
        SeckillResult cached = SeckillResult.success(ACTIVITY_ID, 100L);
        when(valueOps.get("seckill:result:" + ACTIVITY_ID + ":" + USER_ID))
                .thenReturn(cached);

        SeckillResult result = seckillService.getSeckillResult(USER_ID, ACTIVITY_ID);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(SeckillResult.STATUS_SUCCESS);
        assertThat(result.getOrderId()).isEqualTo(100L);
        // 不应该查 DB
        verify(orderService, never()).getUserOrder(anyLong(), anyLong());
    }

    @Test
    void shouldQueryDbAndCacheWhenNoRedisCache() {
        // Redis 无缓存
        when(valueOps.get("seckill:result:" + ACTIVITY_ID + ":" + USER_ID))
                .thenReturn(null);
        // DB 查到订单
        com.lightningdeal.order.entity.SeckillOrder order =
                new com.lightningdeal.order.entity.SeckillOrder();
        order.setId(200L);
        when(orderService.getUserOrder(USER_ID, ACTIVITY_ID)).thenReturn(order);

        SeckillResult result = seckillService.getSeckillResult(USER_ID, ACTIVITY_ID);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(SeckillResult.STATUS_SUCCESS);
        assertThat(result.getOrderId()).isEqualTo(200L);
        // 验证结果被缓存到 Redis
        verify(valueOps).set(eq("seckill:result:" + ACTIVITY_ID + ":" + USER_ID),
                any(SeckillResult.class), anyLong(), any());
    }

    @Test
    void shouldReturnNullWhenNoResultFound() {
        when(valueOps.get("seckill:result:" + ACTIVITY_ID + ":" + USER_ID))
                .thenReturn(null);
        when(orderService.getUserOrder(USER_ID, ACTIVITY_ID)).thenReturn(null);

        SeckillResult result = seckillService.getSeckillResult(USER_ID, ACTIVITY_ID);

        assertThat(result).isNull();
    }
}
