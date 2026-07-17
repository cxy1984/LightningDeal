package com.lightningdeal.common.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BizException 单元测试
 *
 * 测试目标：
 * 1. 双参构造器（code + message）正确设置
 * 2. 单参构造器（message）默认 code=500
 * 3. 异常可作为 RuntimeException 抛出和捕获
 */
class BizExceptionTest {

    @Test
    void shouldCreateWithCodeAndMessage() {
        BizException e = new BizException(400, "参数错误");

        assertThat(e.getCode()).isEqualTo(400);
        assertThat(e.getMessage()).isEqualTo("参数错误");
    }

    @Test
    void shouldCreateWithMessageOnly() {
        BizException e = new BizException("业务异常");

        assertThat(e.getCode()).isEqualTo(500);
        assertThat(e.getMessage()).isEqualTo("业务异常");
    }

    @Test
    void shouldBeRuntimeException() {
        BizException e = new BizException(400, "test");

        assertThat(e).isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldBeThrownAndCaught() {
        try {
            throw new BizException(403, "权限不足");
        } catch (BizException e) {
            assertThat(e.getCode()).isEqualTo(403);
            assertThat(e.getMessage()).isEqualTo("权限不足");
        }
    }

    @Test
    void rateLimitExceptionShouldHave429() {
        RateLimitException e = new RateLimitException("秒杀太火爆啦");

        assertThat(e.getCode()).isEqualTo(429);
        assertThat(e.getMessage()).isEqualTo("秒杀太火爆啦");
        assertThat(e).isInstanceOf(BizException.class);
    }

    @Test
    void rateLimitExceptionShouldBeThrownAndCaught() {
        try {
            throw new RateLimitException("请稍后再试");
        } catch (RateLimitException e) {
            assertThat(e.getCode()).isEqualTo(429);
            assertThat(e.getMessage()).isEqualTo("请稍后再试");
        }
    }
}
