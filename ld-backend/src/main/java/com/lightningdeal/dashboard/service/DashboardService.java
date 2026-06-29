package com.lightningdeal.dashboard.service;

import com.lightningdeal.dashboard.model.DashboardData;

/**
 * 数据大屏服务
 */
public interface DashboardService {

    /**
     * 获取大屏实时数据
     */
    DashboardData getDashboardData();

    /**
     * 记录一次抢购（用于统计 QPS）
     */
    void recordFlash(boolean success, String goodsName, String username);

    /**
     * 获取当前 QPS
     */
    long getCurrentQps();
}
