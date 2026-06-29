package com.lightningdeal.activity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lightningdeal.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀活动实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("seckill_activity")
@Schema(description = "秒杀活动")
public class SeckillActivity extends BaseEntity {

    @Schema(description = "活动名称")
    private String name;

    @Schema(description = "商品ID")
    private Long goodsId;

    @Schema(description = "商品名称")
    private String goodsName;

    @Schema(description = "商品图片URL")
    private String goodsImage;

    @Schema(description = "商品描述")
    private String goodsDescription;

    @Schema(description = "原价")
    private BigDecimal originalPrice;

    @Schema(description = "秒杀价")
    private BigDecimal flashPrice;

    @Schema(description = "秒杀库存总量")
    private Integer totalStock;

    @Schema(description = "已秒杀数量")
    private Integer soldStock;

    @Schema(description = "每人限购数量")
    private Integer limitPerUser;

    @Schema(description = "活动开始时间")
    private LocalDateTime startTime;

    @Schema(description = "活动结束时间")
    private LocalDateTime endTime;

    @Schema(description = "活动状态: 0-草稿 1-上架 2-进行中 3-已结束")
    private Integer status;
}
