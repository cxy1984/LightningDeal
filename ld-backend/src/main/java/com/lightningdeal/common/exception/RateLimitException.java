package com.lightningdeal.common.exception;

/**
 * 限流异常（HTTP 429 Too Many Requests）
 */
public class RateLimitException extends BizException {

    public RateLimitException(String message) {
        super(429, message);
    }
}
