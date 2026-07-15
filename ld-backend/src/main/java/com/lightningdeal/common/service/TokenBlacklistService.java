package com.lightningdeal.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Token 黑名单服务
 * <p>
 * 用于主动失效 JWT：修改密码、踢人、管理员强制下线等场景。
 * 将 refreshToken 加入黑名单，TTL 与 refreshToken 过期时间一致。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "token:blacklist:";

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 将 refreshToken 加入黑名单
     *
     * @param refreshToken 要被吊销的 refreshToken
     * @param ttlSeconds   过期时间（秒），与 refreshToken 的剩余有效期一致
     */
    public void addToBlacklist(String refreshToken, long ttlSeconds) {
        String key = BLACKLIST_PREFIX + refreshToken;
        redisTemplate.opsForValue().set(key, "1", ttlSeconds, TimeUnit.SECONDS);
        log.info("Token 已加入黑名单 ttl={}s", ttlSeconds);
    }

    /**
     * 检查 refreshToken 是否在黑名单中
     */
    public boolean isBlacklisted(String refreshToken) {
        Boolean exists = redisTemplate.hasKey(BLACKLIST_PREFIX + refreshToken);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * 将某用户的所有 refreshToken 加入黑名单（修改密码/踢人时调用）
     * <p>
     * 使用用户维度 key 记录版本号（当前时间戳 + 1秒），确保覆盖所有已签发 token。
     */
    public void invalidateUserTokens(Long userId, long ttlSeconds) {
        String key = BLACKLIST_PREFIX + "user:" + userId;
        long version = System.currentTimeMillis() + 1000; // +1s 确保覆盖
        redisTemplate.opsForValue().set(key, String.valueOf(version), ttlSeconds, TimeUnit.SECONDS);
        log.info("用户 {} 的所有 Token 已失效 version={}", userId, version);
    }

    /**
     * 获取用户 token 版本号（用于校验）
     */
    public String getUserTokenVersion(Long userId) {
        String key = BLACKLIST_PREFIX + "user:" + userId;
        Object val = redisTemplate.opsForValue().get(key);
        return val != null ? val.toString() : null;
    }
}
