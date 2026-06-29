package com.lightningdeal.dashboard.controller;

import com.lightningdeal.common.response.R;
import com.lightningdeal.dashboard.model.DashboardData;
import com.lightningdeal.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据大屏控制器
 */
@Tag(name = "数据大屏", description = "实时监控数据接口")
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "获取大屏实时数据")
    @GetMapping("/data")
    public R<DashboardData> getDashboardData() {
        return R.ok(dashboardService.getDashboardData());
    }

    @Operation(summary = "获取当前 QPS")
    @GetMapping("/qps")
    public R<Long> getQps() {
        return R.ok(dashboardService.getCurrentQps());
    }
}
