package com.lightningdeal.common.annotation;

import java.lang.annotation.*;

/**
 * {@link RateLimit} 的容器注解——支持在同一个方法上标注多个 @RateLimit
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimits {
    RateLimit[] value();
}
