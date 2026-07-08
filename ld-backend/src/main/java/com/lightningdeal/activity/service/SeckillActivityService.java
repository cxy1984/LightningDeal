package com.lightningdeal.activity.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lightningdeal.activity.entity.SeckillActivity;
import com.lightningdeal.activity.model.ActivityVO;

/**
 * 秒杀活动服务
 */
public interface SeckillActivityService extends IService<SeckillActivity> {

    /**
     * 分页查询活动列表
     */
    IPage<ActivityVO> listActivities(int page, int size, Integer status, String name);

    /**
     * 获取活动详情
     */
    ActivityVO getActivityDetail(Long activityId);

    /**
     * 创建活动
     */
    ActivityVO createActivity(SeckillActivity activity);

    /**
     * 更新活动
     */
    ActivityVO updateActivity(SeckillActivity activity);

    /**
     * 删除活动（逻辑删除）
     */
    void deleteActivity(Long activityId);

    /**
     * 更新活动状态
     */
    ActivityVO updateStatus(Long activityId, Integer status);

    /**
     * 预热活动库存到 Redis
     */
    void preheatStock(Long activityId);

    /**
     * 扣减数据库库存（乐观锁）
     */
    boolean decrementDbStock(Long activityId);

    /**
     * 取消订单时回退已售数量（sold_stock - 1）
     */
    void revertSoldStock(Long activityId);

    /**
     * Redis 回补库存（下单失败或取消订单时使用）
     */
    void incrRedisStock(Long activityId);
}
