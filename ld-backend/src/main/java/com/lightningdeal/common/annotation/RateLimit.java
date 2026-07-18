package com.lightningdeal.common.annotation;

import java.lang.annotation.*;

/**
 * 限流注解（可重复，支持多层限流）
 * <p>
 * 基于 Redis 令牌桶算法实现，支持 SpEL 动态 key。
 * 可在同一个方法上标注多个 {@code @RateLimit} 实现双层/多层限流。
 * <p>
 * 示例：
 * <pre>{@code
 * // 双层限流：全局限流 + 用户级限流
 * @RateLimit(qps = 200, capacity = 300, key = "'seckill:' + #request.activityId",
 *            message = "秒杀太火爆啦")
 * @RateLimit(qps = 1, capacity = 2, key = "'seckill:' + #request.activityId + ':' + #authentication.principal",
 *            message = "操作太频繁，请稍后再试")
 * public Result execute(SeckillRequest request, Authentication auth) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(RateLimits.class)
public @interface RateLimit {

    /** 每秒填充令牌数（即 QPS 上限） */
    int qps() default 100;

    /** 桶最大容量（允许瞬间突发的峰值，建议 >= qps） */
    int capacity() default 200;

    /**
     * 限流维度 key（SpEL 表达式）
     * <ul>
     *   <li>支持通过 #参数名 引用方法参数</li>
     *   <li>支持级联属性： #request.activityId</li>
     *   <li>支持字符串拼接： 'prefix:' + #id</li>
     * </ul>
     * 最终 key 格式： rate_limit:{key}
     */
    String key() default "";

    /** 被限流时的提示文案 */
    String message() default "请求过于频繁，请稍后重试";
}
