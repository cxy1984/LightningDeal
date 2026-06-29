package com.lightningdeal.seckill.service;

import com.lightningdeal.seckill.model.SeckillRequest;
import com.lightningdeal.seckill.model.SeckillResult;

/**
 * 秒杀核心服务
 */
public interface SeckillService {

    /**
     * 执行秒杀
     * 流程：Redis预减库存 → 校验重复秒杀 → 发送MQ消息 → 返回排队中
     */
    SeckillResult executeSeckill(Long userId, SeckillRequest request);

    /**
     * 获取秒杀结果（轮询/WebSocket推送）
     */
    SeckillResult getSeckillResult(Long userId, Long activityId);
}
