package com.lightningdeal.seckill.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 秒杀请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "秒杀请求")
public class SeckillRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "活动ID", example = "1")
    private Long activityId;

    @Schema(description = "购买数量", example = "1")
    private Integer quantity;
}
