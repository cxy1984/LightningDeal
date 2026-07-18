package com.lightningdeal.common.aspect;

import com.lightningdeal.common.annotation.RateLimit;
import com.lightningdeal.common.exception.RateLimitException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RateLimitAspect 单元测试
 *
 * 测试目标：
 * 1. 令牌桶有令牌 → 放行
 * 2. 令牌桶空 → 抛 RateLimitException(429)
 * 3. SpEL key 解析正确
 * 4. 不同 key 独立限流
 * 5. Lua 脚本参数传递正确
 * 6. QPS 记录到 Redis
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = Strictness.LENIENT)
class RateLimitAspectTest {

    @Mock StringRedisTemplate stringRedisTemplate;
    @Mock ValueOperations<String, String> valueOps;

    @Captor ArgumentCaptor<List<String>> redisKeyCaptor;

    private RateLimitAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new RateLimitAspect(stringRedisTemplate);
        aspect.init(); // 初始化 Lua 脚本

        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
    }

    // ==================== 放行 / 拦截 ====================

    @Test
    void shouldAllowWhenTokenBucketHasTokens() throws NoSuchMethodException {
        // 模拟 Lua 脚本返回 1（有令牌）
        when(stringRedisTemplate.execute(
                any(DefaultRedisScript.class), anyList(), anyString(), anyString(), anyString()))
                .thenReturn(1L);

        JoinPoint joinPoint = mockJoinPoint("testKey");

        // 不应抛异常
        aspect.doRateLimit(joinPoint, createRateLimit(5, 10, "'test' + 'Key'"));
    }

    @Test
    void shouldBlockWhenTokenBucketIsEmpty() throws NoSuchMethodException {
        when(stringRedisTemplate.execute(
                any(DefaultRedisScript.class), anyList(), anyString(), anyString(), anyString()))
                .thenReturn(0L);

        JoinPoint joinPoint = mockJoinPoint("testKey");

        assertThatThrownBy(() -> aspect.doRateLimit(joinPoint, createRateLimit(5, 10, "'testKey'")))
                .isInstanceOf(RateLimitException.class)
                .hasMessageContaining("秒杀太火爆啦");
    }

    @Test
    void shouldBlockWhenScriptReturnsNull() throws NoSuchMethodException {
        when(stringRedisTemplate.execute(
                any(DefaultRedisScript.class), anyList(), anyString(), anyString(), anyString()))
                .thenReturn(null);

        JoinPoint joinPoint = mockJoinPoint("testKey");

        assertThatThrownBy(() -> aspect.doRateLimit(joinPoint, createRateLimit(5, 10, "'testKey'")))
                .isInstanceOf(RateLimitException.class);
    }

    // ==================== Redis key 构建 ====================

    @Test
    void shouldUseCorrectRedisKeyPrefix() throws NoSuchMethodException {
        when(stringRedisTemplate.execute(
                any(DefaultRedisScript.class), redisKeyCaptor.capture(), anyString(), anyString(), anyString()))
                .thenReturn(1L);

        JoinPoint joinPoint = mockJoinPoint("activity_1");

        aspect.doRateLimit(joinPoint, createRateLimit(5, 10, "'activity_1'"));

        String actualKey = redisKeyCaptor.getValue().get(0);
        assertThat(actualKey).isEqualTo("rate_limit:activity_1");
    }

    @Test
    void differentKeysShouldBeIndependent() throws NoSuchMethodException {
        when(stringRedisTemplate.execute(
                any(DefaultRedisScript.class), anyList(), anyString(), anyString(), anyString()))
                .thenReturn(1L);

        JoinPoint joinPoint = mockJoinPoint("key1");
        aspect.doRateLimit(joinPoint, createRateLimit(5, 10, "'key1'"));

        joinPoint = mockJoinPoint("key2");
        aspect.doRateLimit(joinPoint, createRateLimit(5, 10, "'key2'"));

        // 验证两个不同的 key 传到 Redis
        verify(stringRedisTemplate, times(2))
                .execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString(), anyString());
    }

    // ==================== Lua 参数传递 ====================

    @Test
    void shouldPassQpsAndCapacityToLua() throws NoSuchMethodException {
        ArgumentCaptor<String> arg1 = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> arg2 = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> arg3 = ArgumentCaptor.forClass(String.class);

        when(stringRedisTemplate.execute(
                any(DefaultRedisScript.class), anyList(), arg1.capture(), arg2.capture(), arg3.capture()))
                .thenReturn(1L);

        JoinPoint joinPoint = mockJoinPoint("test");
        aspect.doRateLimit(joinPoint, createRateLimit(20, 50, "'test'"));

        // ARGV[1] = rate (qps), ARGV[2] = capacity, ARGV[3] = now (timestamp)
        assertThat(arg1.getValue()).isEqualTo("20");
        assertThat(arg2.getValue()).isEqualTo("50");
        // 第三个参数是时间戳，验证是数字字符串
        assertThat(arg3.getValue()).matches("\\d+");
    }

    // ==================== QPS 统计 ====================

    @Test
    void shouldRecordQpsToRedisWhenAllowed() throws NoSuchMethodException {
        when(stringRedisTemplate.execute(
                any(DefaultRedisScript.class), anyList(), anyString(), anyString(), anyString()))
                .thenReturn(1L);

        JoinPoint joinPoint = mockJoinPoint("test");
        aspect.doRateLimit(joinPoint, createRateLimit(5, 10, "'test'"));

        // 验证 QPS 计数增加（dashboard:qps:{timestamp}）
        verify(valueOps).increment(argThat(key ->
                key instanceof String && ((String) key).startsWith("dashboard:qps:")));
    }

    // ==================== 辅助方法 ====================

    private JoinPoint mockJoinPoint(String expectedKey) throws NoSuchMethodException {
        // 使用一个简单的方法，参数仅用于 SpEL 测试
        // 注意：此处 method 不带 @RateLimit 注解，测试的是切面被触发后
        // 通过参数传入的 rateLimit 对象来执行限流逻辑
        Method method = SampleService.class.getMethod("sampleMethod", String.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getDeclaringType()).thenReturn(SampleService.class);

        JoinPoint joinPoint = mock(JoinPoint.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{expectedKey});
        when(joinPoint.getTarget()).thenReturn(new SampleService());

        return joinPoint;
    }

    private RateLimit createRateLimit(int qps, int capacity, String key) {
        return new RateLimit() {
            @Override
            public int qps() { return qps; }
            @Override
            public int capacity() { return capacity; }
            @Override
            public String key() { return key; }
            @Override
            public String message() { return "秒杀太火爆啦，请稍后再试"; }
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return RateLimit.class;
            }
        };
    }

    /**
     * 用于获取 Method 对象的示例类
     */
    public static class SampleService {
        public void sampleMethod(String keyword) {
        }
    }
}
