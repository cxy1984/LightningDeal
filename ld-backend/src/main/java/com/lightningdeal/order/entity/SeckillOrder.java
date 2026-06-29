package com.lightningdeal.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lightningdeal.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀订单实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("seckill_order")
@Schema(description = "秒杀订单")
public class SeckillOrder extends BaseEntity {

    @Schema(description = "订单编号")
    private String orderNo;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "活动ID")
    private Long activityId;

    @Schema(description = "商品名称")
    private String goodsName;

    @Schema(description = "商品图片")
    private String goodsImage;

    @Schema(description = "秒杀价格")
    private BigDecimal flashPrice;

    @Schema(description = "购买数量")
    private Integer quantity;

    @Schema(description = "订单总金额")
    private BigDecimal totalAmount;

    @Schema(description = "订单状态: 0-待支付 1-已支付 2-已取消 3-已退款")
    private Integer status;

    @Schema(description = "支付时间")
    private LocalDateTime payTime;
}
