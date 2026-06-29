package com.lightningdeal.order.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lightningdeal.common.response.R;
import com.lightningdeal.order.model.OrderVO;
import com.lightningdeal.order.service.SeckillOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 订单控制器
 */
@Tag(name = "订单管理", description = "订单查询、支付、取消")
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class SeckillOrderController {

    private final SeckillOrderService orderService;

    @Operation(summary = "我的订单列表")
    @GetMapping("/list")
    public R<IPage<OrderVO>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "状态筛选") @RequestParam(required = false) Integer status,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return R.ok(orderService.getUserOrders(userId, page, size, status));
    }

    @Operation(summary = "支付订单")
    @PostMapping("/pay/{orderId}")
    public R<String> pay(@PathVariable Long orderId) {
        orderService.payOrder(orderId);
        return R.ok("支付成功");
    }

    @Operation(summary = "取消订单")
    @PostMapping("/cancel/{orderId}")
    public R<String> cancel(@PathVariable Long orderId) {
        orderService.cancelOrder(orderId);
        return R.ok("已取消");
    }
}
