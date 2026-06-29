package com.lightningdeal.activity.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 活动视图对象
 */
@Data
@Schema(description = "活动视图")
public class ActivityVO {

    @Schema(description = "活动ID")
    private Long id;

    @Schema(description = "活动名称")
    private String name;

    @Schema(description = "商品ID")
    private Long goodsId;

    @Schema(description = "商品名称")
    private String goodsName;

    @Schema(description = "商品图片")
    private String goodsImage;

    @Schema(description = "商品描述")
    private String goodsDescription;

    @Schema(description = "原价")
    private BigDecimal originalPrice;

    @Schema(description = "秒杀价")
    private BigDecimal flashPrice;

    @Schema(description = "总库存")
    private Integer totalStock;

    @Schema(description = "已售数量")
    private Integer soldStock;

    @Schema(description = "剩余库存")
    private Integer remainStock;

    @Schema(description = "每人限购")
    private Integer limitPerUser;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "状态: 0-草稿 1-上架 2-进行中 3-已结束")
    private Integer status;

    @Schema(description = "是否正在倒计时")
    private boolean countingDown;

    @Schema(description = "倒计时剩余毫秒（开始前为正数，结束后为0）")
    private long countdownMillis;
}
