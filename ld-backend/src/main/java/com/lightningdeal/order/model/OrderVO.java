package com.lightningdeal.order.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单视图
 */
@Data
@Schema(description = "订单信息")
public class OrderVO {

    @Schema(description = "订单ID")
    private Long id;

    @Schema(description = "订单编号")
    private String orderNo;

    @Schema(description = "活动ID")
    private Long activityId;

    @Schema(description = "商品名称")
    private String goodsName;

    @Schema(description = "商品图片")
    private String goodsImage;

    @Schema(description = "秒杀价")
    private BigDecimal flashPrice;

    @Schema(description = "数量")
    private Integer quantity;

    @Schema(description = "总金额")
    private BigDecimal totalAmount;

    @Schema(description = "状态: 0-待支付 1-已支付 2-已取消 3-已退款")
    private Integer status;

    @Schema(description = "状态文本")
    private String statusText;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
