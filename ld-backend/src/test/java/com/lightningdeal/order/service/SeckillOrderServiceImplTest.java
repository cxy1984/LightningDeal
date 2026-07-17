package com.lightningdeal.order.service;

import com.lightningdeal.activity.service.SeckillActivityService;
import com.lightningdeal.order.entity.SeckillOrder;
import com.lightningdeal.order.mapper.SeckillOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SeckillOrderServiceImpl 单元测试
 *
 * 测试目标：
 * 1. createOrder 创建订单，状态为待支付
 * 2. cancelOrder 取消订单 → 状态 2，Redis 回退 + sold_stock 回退
 * 3. refundOrder 已支付退款 → 状态 3，Redis 恢复
 * 4. payOrder 支付 → 状态 1，记录支付时间
 * 5. 边界校验：订单不存在/状态异常
 * 6. getUserOrder / countUserOrders
 */
@ExtendWith(MockitoExtension.class)
class SeckillOrderServiceImplTest {

    private static final Long USER_ID = 42L;
    private static final Long ACTIVITY_ID = 1L;
    private static final Long ORDER_ID = 100L;

    @Mock SeckillOrderMapper orderMapper;
    @Mock SeckillActivityService activityService;
    @Mock RedisTemplate<String, Object> redisTemplate;
    @Mock ValueOperations<String, Object> valueOps;
    @Mock SetOperations<String, Object> setOps;

    private SeckillOrderServiceImpl orderService;

    private SeckillOrder unpaidOrder;
    private SeckillOrder paidOrder;

    @BeforeEach
    void setUp() throws Exception {
        // 手动构造 ServiceImpl，注入 mock 的 mapper
        orderService = new SeckillOrderServiceImpl(activityService, redisTemplate);
        // 反射设置 baseMapper（ServiceImpl 的 setBaseMapper 是 protected）
        Field mapperField = com.baomidou.mybatisplus.extension.service.impl.ServiceImpl.class
                .getDeclaredField("baseMapper");
        mapperField.setAccessible(true);
        mapperField.set(orderService, orderMapper);

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOps);

        // 待支付订单
        unpaidOrder = new SeckillOrder();
        unpaidOrder.setId(ORDER_ID);
        unpaidOrder.setUserId(USER_ID);
        unpaidOrder.setActivityId(ACTIVITY_ID);
        unpaidOrder.setOrderNo("LD123456789");
        unpaidOrder.setGoodsName("测试商品");
        unpaidOrder.setFlashPrice(BigDecimal.valueOf(99));
        unpaidOrder.setQuantity(1);
        unpaidOrder.setTotalAmount(BigDecimal.valueOf(99));
        unpaidOrder.setStatus(0); // 待支付
        unpaidOrder.setCreateTime(LocalDateTime.now());

        // 已支付订单
        paidOrder = new SeckillOrder();
        paidOrder.setId(ORDER_ID);
        paidOrder.setUserId(USER_ID);
        paidOrder.setActivityId(ACTIVITY_ID);
        paidOrder.setStatus(1); // 已支付
        paidOrder.setPayTime(LocalDateTime.now());
    }

    // ==================== createOrder ====================

    @Test
    void shouldCreateOrderWithPendingStatus() {
        when(activityService.getById(ACTIVITY_ID)).thenReturn(
                createMockActivity("测试商品"));

        SeckillOrder order = orderService.createOrder(USER_ID, ACTIVITY_ID, 1);

        assertThat(order.getUserId()).isEqualTo(USER_ID);
        assertThat(order.getActivityId()).isEqualTo(ACTIVITY_ID);
        assertThat(order.getStatus()).isEqualTo(0); // 待支付
        assertThat(order.getOrderNo()).startsWith("LD");
        assertThat(order.getGoodsName()).isEqualTo("测试商品");
        assertThat(order.getFlashPrice()).isEqualByComparingTo(BigDecimal.valueOf(9900));
        assertThat(order.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(9900));
        assertThat(order.getQuantity()).isEqualTo(1);

        verify(orderMapper).insert(order);
    }

    @Test
    void shouldCalculateTotalAmountByQuantity() {
        when(activityService.getById(ACTIVITY_ID)).thenReturn(
                createMockActivity("多件商品"));

        SeckillOrder order = orderService.createOrder(USER_ID, ACTIVITY_ID, 3);

        assertThat(order.getTotalAmount())
                .isEqualByComparingTo(BigDecimal.valueOf(9900 * 3));
        assertThat(order.getQuantity()).isEqualTo(3);
    }

    @Test
    void shouldThrowWhenActivityNotFoundForCreate() {
        when(activityService.getById(ACTIVITY_ID)).thenReturn(null);

        assertThatThrownBy(() -> orderService.createOrder(USER_ID, ACTIVITY_ID, 1))
                .hasMessageContaining("活动不存在");
    }

    // ==================== cancelOrder ====================

    @Test
    void shouldCancelUnpaidOrderAndRecoverResources() {
        when(orderMapper.selectById(ORDER_ID)).thenReturn(unpaidOrder);

        orderService.cancelOrder(ORDER_ID);

        // 验证订单状态变为已取消
        assertThat(unpaidOrder.getStatus()).isEqualTo(2);
        verify(orderMapper).updateById(unpaidOrder);

        // 验证 Redis 计数递减
        verify(valueOps).decrement("seckill:user_count:" + ACTIVITY_ID + ":" + USER_ID);

        // 验证 sold_stock 回退
        verify(activityService).revertSoldStock(ACTIVITY_ID);

        // 验证 Redis 库存恢复
        verify(valueOps).increment("seckill:stock:" + ACTIVITY_ID);

        // 验证用户标记移除
        verify(setOps).remove("seckill:users:" + ACTIVITY_ID, USER_ID.toString());
    }

    @Test
    void shouldDoNothingWhenOrderNotFoundForCancel() {
        when(orderMapper.selectById(ORDER_ID)).thenReturn(null);

        orderService.cancelOrder(ORDER_ID);

        // 不应执行任何更新或资源恢复
        verify(valueOps, never()).decrement(anyString());
        verify(activityService, never()).revertSoldStock(anyLong());
        verify(valueOps, never()).increment(anyString());
        verify(setOps, never()).remove(anyString(), anyString());
    }

    // ==================== refundOrder ====================

    @Test
    void shouldRefundPaidOrder() {
        when(orderMapper.selectById(ORDER_ID)).thenReturn(paidOrder);

        orderService.refundOrder(ORDER_ID);

        // 状态变为已退款
        assertThat(paidOrder.getStatus()).isEqualTo(3);
        verify(orderMapper).updateById(paidOrder);

        // Redis 计数递减
        verify(valueOps).decrement("seckill:user_count:" + ACTIVITY_ID + ":" + USER_ID);

        // sold_stock 回退
        verify(activityService).revertSoldStock(ACTIVITY_ID);

        // Redis 库存恢复
        verify(valueOps).increment("seckill:stock:" + ACTIVITY_ID);
    }

    @Test
    void shouldThrowWhenRefundingUnpaidOrder() {
        when(orderMapper.selectById(ORDER_ID)).thenReturn(unpaidOrder);

        assertThatThrownBy(() -> orderService.refundOrder(ORDER_ID))
                .hasMessageContaining("仅已支付订单可退款");
    }

    @Test
    void shouldThrowWhenRefundingNonexistentOrder() {
        when(orderMapper.selectById(ORDER_ID)).thenReturn(null);

        assertThatThrownBy(() -> orderService.refundOrder(ORDER_ID))
                .hasMessageContaining("订单不存在");
    }

    // ==================== payOrder ====================

    @Test
    void shouldPayUnpaidOrder() {
        when(orderMapper.selectById(ORDER_ID)).thenReturn(unpaidOrder);

        orderService.payOrder(ORDER_ID);

        assertThat(unpaidOrder.getStatus()).isEqualTo(1);
        assertThat(unpaidOrder.getPayTime()).isNotNull();
        verify(orderMapper).updateById(unpaidOrder);
    }

    @Test
    void shouldThrowWhenPayingAlreadyPaidOrder() {
        when(orderMapper.selectById(ORDER_ID)).thenReturn(paidOrder);

        assertThatThrownBy(() -> orderService.payOrder(ORDER_ID))
                .hasMessageContaining("订单状态异常");
    }

    @Test
    void shouldThrowWhenPayingNonexistentOrder() {
        when(orderMapper.selectById(ORDER_ID)).thenReturn(null);

        assertThatThrownBy(() -> orderService.payOrder(ORDER_ID))
                .hasMessageContaining("订单不存在");
    }

    // ==================== getUserOrder ====================
    // 注：getUserOrder/countUserOrders 使用了 lambdaQuery() MyBatis-Plus 动态代理，
    // 纯 Mockito 单元测试中无法完全模拟 MyBatis-Plus 的内部代理机制。
    // 这几个方法已在集成测试中覆盖，此处跳过纯 Mock 测试。
    // 关键的业务逻辑（cancel/refund/pay）已在上方覆盖。

    // ==================== 辅助方法 ====================

    private com.lightningdeal.activity.entity.SeckillActivity createMockActivity(String goodsName) {
        com.lightningdeal.activity.entity.SeckillActivity activity =
                new com.lightningdeal.activity.entity.SeckillActivity();
        activity.setId(ACTIVITY_ID);
        activity.setGoodsName(goodsName);
        activity.setFlashPrice(BigDecimal.valueOf(9900));
        activity.setStartTime(LocalDateTime.now().minusMinutes(30));
        activity.setEndTime(LocalDateTime.now().plusMinutes(30));
        return activity;
    }
}
