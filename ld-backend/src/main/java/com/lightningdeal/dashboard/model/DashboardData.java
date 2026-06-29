package com.lightningdeal.dashboard.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 大屏数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "实时大屏数据")
public class DashboardData implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "当前QPS")
    private long currentQps;

    @Schema(description = "峰值QPS")
    private long peakQps;

    @Schema(description = "总订单数")
    private long totalOrders;

    @Schema(description = "成功订单数")
    private long successOrders;

    @Schema(description = "失败订单数")
    private long failOrders;

    @Schema(description = "库存预载总量")
    private int totalPreloadStock;

    @Schema(description = "剩余库存")
    private int remainStock;

    @Schema(description = "QPS 时间序列（近一分钟每秒）")
    private List<Long> qpsHistory;

    @Schema(description = "实时抢购流水")
    private List<FlashItem> flashStream;

    @Schema(description = "商品排行榜")
    private List<RankItem> rankList;

    @Schema(description = "系统状态")
    private String systemStatus;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "实时抢购流水项")
    public static class FlashItem {
        private String username;
        private boolean success;
        private String goodsName;
        private long timestamp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "商品排行项")
    public static class RankItem {
        private String goodsName;
        private long salesCount;
        private int rank;
    }
}
