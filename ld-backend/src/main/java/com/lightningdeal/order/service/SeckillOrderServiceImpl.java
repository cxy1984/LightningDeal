package com.lightningdeal.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lightningdeal.activity.entity.SeckillActivity;
import com.lightningdeal.activity.service.SeckillActivityService;
import com.lightningdeal.common.exception.BizException;
import com.lightningdeal.order.entity.SeckillOrder;
import com.lightningdeal.order.mapper.SeckillOrderMapper;
import com.lightningdeal.order.model.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillOrderServiceImpl extends ServiceImpl<SeckillOrderMapper, SeckillOrder>
        implements SeckillOrderService {

    private final SeckillActivityService activityService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SeckillOrder createOrder(Long userId, Long activityId, Integer quantity) {
        SeckillActivity activity = activityService.getById(activityId);
        if (activity == null) {
            throw new BizException(400, "活动不存在");
        }

        SeckillOrder order = new SeckillOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setActivityId(activityId);
        order.setGoodsName(activity.getGoodsName());
        order.setGoodsImage(activity.getGoodsImage());
        order.setFlashPrice(activity.getFlashPrice());
        order.setQuantity(quantity);
        order.setTotalAmount(activity.getFlashPrice().multiply(BigDecimal.valueOf(quantity)));
        order.setStatus(0); // 待支付

        save(order);
        log.info("订单创建成功 orderId={}, orderNo={}, userId={}", order.getId(), order.getOrderNo(), userId);
        return order;
    }

    @Override
    public SeckillOrder getUserOrder(Long userId, Long activityId) {
        return lambdaQuery()
                .eq(SeckillOrder::getUserId, userId)
                .eq(SeckillOrder::getActivityId, activityId)
                .orderByDesc(SeckillOrder::getCreateTime)
                .last("LIMIT 1")
                .one();
    }

    @Override
    public IPage<OrderVO> getUserOrders(Long userId, int page, int size, Integer status) {
        LambdaQueryWrapper<SeckillOrder> wrapper = new LambdaQueryWrapper<SeckillOrder>()
                .eq(SeckillOrder::getUserId, userId)
                .eq(status != null, SeckillOrder::getStatus, status)
                .orderByDesc(SeckillOrder::getCreateTime);

        IPage<SeckillOrder> entityPage = page(new Page<>(page, size), wrapper);
        return entityPage.convert(this::toVO);
    }

    @Override
    public OrderVO getOrderDetail(Long orderId) {
        SeckillOrder order = getById(orderId);
        if (order == null) {
            throw new BizException(404, "订单不存在");
        }
        return toVO(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(Long orderId) {
        SeckillOrder order = getById(orderId);
        if (order == null) {
            throw new BizException(404, "订单不存在");
        }
        if (order.getStatus() != 0) {
            throw new BizException(400, "订单状态异常");
        }
        order.setStatus(1);
        order.setPayTime(LocalDateTime.now());
        updateById(order);
        log.info("订单支付成功 orderId={}", orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId) {
        SeckillOrder order = getById(orderId);
        if (order == null) return;
        order.setStatus(2);
        updateById(order);
        log.info("订单已取消 orderId={}", orderId);
    }

    private OrderVO toVO(SeckillOrder order) {
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);
        switch (order.getStatus()) {
            case 0: vo.setStatusText("待支付"); break;
            case 1: vo.setStatusText("已支付"); break;
            case 2: vo.setStatusText("已取消"); break;
            case 3: vo.setStatusText("已退款"); break;
            default: vo.setStatusText("未知");
        }
        return vo;
    }

    @Override
    public long countUserOrders(Long userId, Long activityId) {
        return lambdaQuery()
                .eq(SeckillOrder::getUserId, userId)
                .eq(SeckillOrder::getActivityId, activityId)
                .ne(SeckillOrder::getStatus, 2) // 排除已取消
                .count();
    }

    private String generateOrderNo() {
        return "LD" + System.currentTimeMillis() + (int) (Math.random() * 1000);
    }
}
