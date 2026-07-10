package com.lightningdeal.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lightningdeal.order.entity.SeckillOrder;
import com.lightningdeal.order.model.OrderVO;
import java.math.BigDecimal;

/**
 * 订单服务
 */
public interface SeckillOrderService extends IService<SeckillOrder> {

    /**
     * 创建订单（MQ 消费者调用）
     */
    SeckillOrder createOrder(Long userId, Long activityId, Integer quantity);

    /**
     * 获取用户某个活动的订单
     */
    SeckillOrder getUserOrder(Long userId, Long activityId);

    /**
     * 分页查询用户订单
     */
    IPage<OrderVO> getUserOrders(Long userId, int page, int size, Integer status,
                                 String startDate, String endDate,
                                 BigDecimal minAmount, BigDecimal maxAmount);

    /**
     * 支付订单
     */
    void payOrder(Long orderId);

    /**
     * 取消订单
     */
    void cancelOrder(Long orderId);

    /**
     * 退款（已支付 → 已退款）
     */
    void refundOrder(Long orderId);

    /**
     * 获取订单详情
     */
    OrderVO getOrderDetail(Long orderId);

    /**
     * 统计用户某个活动已购买数量（排除已取消）
     */
    long countUserOrders(Long userId, Long activityId);
}
