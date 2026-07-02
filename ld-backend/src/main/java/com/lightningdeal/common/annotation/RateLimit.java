package com.lightningdeal.common.annotation;

import java.lang.annotation.*;

/**
 * 限流注解
 * <p>
 * 基于 Redis 令牌桶算法实现，支持 SpEL 动态 key。
 * 使用方式：在 Controller 或 Service 方法上标注，配合切面拦截。
 * <p>
 * 示例：
 * <pre>{@code
 * // 按活动+用户维度限流，每个用户每秒最多 5 次
 * @RateLimit(qps = 5, capacity = 10, key = "'seckill:' + #request.activityId + ':' + #authentication.principal")
 * public Result execute(SeckillRequest request, Authentication auth) { ... }
 *
 * // 全局限流，整个接口每秒最多 1000 次
 * @RateLimit(qps = 1000, capacity = 2000, key = "'seckill:global'")
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
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
