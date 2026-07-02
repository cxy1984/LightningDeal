package com.lightningdeal.common.aspect;

import com.lightningdeal.common.annotation.RateLimit;
import com.lightningdeal.common.exception.RateLimitException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.StandardReflectionParameterNameDiscoverer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collections;

/**
 * 限流注解切面 —— 基于 Redis 令牌桶算法
 * <p>
 * 前置拦截，每秒超过 {@link RateLimit#qps()} 个请求的调用直接抛 {@link RateLimitException}(429)，
 * 由 {@link com.lightningdeal.common.exception.GlobalExceptionHandler} 统一处理。
 */
@Slf4j
@Aspect
@Component
public class RateLimitAspect {

    private final StringRedisTemplate stringRedisTemplate;

    /** Redis 令牌桶 Lua 脚本（已缓存在 DefaultRedisScript 中，避免每次调用都传脚本体） */
    private DefaultRedisScript<Long> tokenBucketScript;

    /** SpEL 表达式解析器（线程安全） */
    private static final ExpressionParser SPEL_PARSER = new SpelExpressionParser();

    /** 获取方法参数名的工具 */
    private static final StandardReflectionParameterNameDiscoverer PARAM_NAME_DISCOVERER =
            new StandardReflectionParameterNameDiscoverer();

    private static final String REDIS_KEY_PREFIX = "rate_limit:";

    @Autowired
    public RateLimitAspect(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @PostConstruct
    public void init() {
        // Lua 脚本：令牌桶算法，原子性减令牌
        // KEYS[1] = rate_limit:{key}
        // ARGV[1] = rate（每秒填充数）
        // ARGV[2] = capacity（桶容量）
        //
        // 返回 1 允许通过，返回 0 拒绝
        String lua =
                "local key    = KEYS[1]\n" +
                "local rate    = tonumber(ARGV[1])\n" +
                "local cap     = tonumber(ARGV[2])\n" +
                "local now     = tonumber(ARGV[3])\n" +
                "\n" +
                "local tokens_key = key .. ':tokens'\n" +
                "local last_key   = key .. ':last'\n" +
                "\n" +
                "local last = redis.call('GET', last_key)\n" +
                "if last == false then\n" +
                "    -- 首次初始化：令牌桶满，并消耗 1 个令牌\n" +
                "    redis.call('SET', tokens_key, cap - 1)\n" +
                "    redis.call('SET', last_key, now)\n" +
                "    local ttl = math.ceil(cap / rate) + 1\n" +
                "    redis.call('EXPIRE', tokens_key, ttl)\n" +
                "    redis.call('EXPIRE', last_key, ttl)\n" +
                "    return 1\n" +
                "end\n" +
                "\n" +
                "local tokens = tonumber(redis.call('GET', tokens_key))\n" +
                "if tokens == nil then\n" +
                "    tokens = cap\n" +
                "end\n" +
                "\n" +
                "local elapsed = math.max(0, now - last)\n" +
                "-- 根据时间差补充令牌\n" +
                "local new_tokens = math.floor(elapsed * rate / 1000)\n" +
                "tokens = tokens + new_tokens\n" +
                "if tokens > cap then\n" +
                "    tokens = cap\n" +
                "end\n" +
                "\n" +
                "if tokens < 1 then\n" +
                "    return 0\n" +
                "end\n" +
                "\n" +
                "tokens = tokens - 1\n" +
                "redis.call('SET', tokens_key, tokens)\n" +
                "redis.call('SET', last_key, now)\n" +
                "\n" +
                "local ttl = math.ceil(cap / rate) + 1\n" +
                "redis.call('EXPIRE', tokens_key, ttl)\n" +
                "redis.call('EXPIRE', last_key, ttl)\n" +
                "\n" +
                "return 1";

        tokenBucketScript = new DefaultRedisScript<>(lua, Long.class);
    }

    @Before("@annotation(rateLimit)")
    public void doRateLimit(JoinPoint joinPoint, RateLimit rateLimit) {
        // 1. 解析 SpEL key
        String key = parseKey(rateLimit.key(), joinPoint);
        String redisKey = REDIS_KEY_PREFIX + key;

        // 2. 执行令牌桶 Luas
        Long now = System.currentTimeMillis();
        Long allowed = stringRedisTemplate.execute(
                tokenBucketScript,
                Collections.singletonList(redisKey),
                String.valueOf(rateLimit.qps()),
                String.valueOf(rateLimit.capacity()),
                String.valueOf(now)
        );

        // 3. 判断
        if (allowed == null || allowed == 0) {
            log.warn("❌ 限流触发 key={}, qps={}, capacity={}", redisKey, rateLimit.qps(), rateLimit.capacity());
            throw new RateLimitException(rateLimit.message());
        }

        log.debug("✅ 限流放行 key={}", redisKey);
    }

    /**
     * 解析 SpEL 表达式为实际的 key 值
     */
    private String parseKey(String keyExpr, JoinPoint joinPoint) {
        if (keyExpr == null || keyExpr.isEmpty()) {
            throw new IllegalArgumentException("@RateLimit key 不能为空");
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Object[] args = joinPoint.getArgs();

        // 构造 Spring 方法执行上下文（支持 #参数名 引用，需 compile -parameters）
        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                null, signature.getMethod(), args, PARAM_NAME_DISCOVERER);

        return SPEL_PARSER.parseExpression(keyExpr).getValue(context, String.class);
    }
}
