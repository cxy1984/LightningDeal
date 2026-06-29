package com.lightningdeal.seckill.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 秒杀结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "秒杀结果")
public class SeckillResult implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "是否成功")
    private boolean success;

    @Schema(description = "消息")
    private String message;

    @Schema(description = "订单ID（成功时返回）")
    private Long orderId;

    @Schema(description = "活动ID")
    private Long activityId;

    @Schema(description = "秒杀状态: 1-排队中 2-成功 3-失败 4-重复秒杀")
    private Integer status;

    // 状态常量
    public static final int STATUS_QUEUING = 1;
    public static final int STATUS_SUCCESS = 2;
    public static final int STATUS_FAIL = 3;
    public static final int STATUS_REPEAT = 4;

    public static SeckillResult queuing(Long activityId) {
        return SeckillResult.builder()
                .success(true).message("排队中").activityId(activityId)
                .status(STATUS_QUEUING).build();
    }

    public static SeckillResult success(Long activityId, Long orderId) {
        return SeckillResult.builder()
                .success(true).message("抢购成功！").activityId(activityId)
                .orderId(orderId).status(STATUS_SUCCESS).build();
    }

    public static SeckillResult fail(String msg, Long activityId) {
        return SeckillResult.builder()
                .success(false).message(msg).activityId(activityId)
                .status(STATUS_FAIL).build();
    }

    public static SeckillResult repeat(Long activityId) {
        return SeckillResult.builder()
                .success(false).message("您已参与过该活动").activityId(activityId)
                .status(STATUS_REPEAT).build();
    }
}
