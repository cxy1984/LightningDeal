package com.lightningdeal.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JwtUtil 单元测试
 *
 * 测试目标：
 * 1. 签发 token 后能正确解析回 userId/username/role
 * 2. 过期的 token 应校验失败
 * 3. 被篡改的 token 应校验失败
 * 4. refreshToken 具有更长的有效期
 * 5. 签发时间正确
 */
class JwtUtilTest {

    /** 测试用密钥——必须 >= 32 字节（HMAC-SHA 要求 256 bits） */
    private static final String TEST_SECRET =
            "TestSecretKey2024!@#$TestSecretKey2024!@#$ABCDEFGH";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        // accessToken 1 小时，refreshToken 1 天
        jwtUtil = new JwtUtil(TEST_SECRET, 3600000L, 86400000L);
    }

    // ==================== accessToken ====================

    @Test
    void shouldGenerateAndParseAccessToken() {
        String token = jwtUtil.generateToken(1L, "admin", "ADMIN");

        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.getUserIdFromToken(token)).isEqualTo(1L);
        assertThat(jwtUtil.getUsernameFromToken(token)).isEqualTo("admin");
        assertThat(jwtUtil.getRoleFromToken(token)).isEqualTo("ADMIN");
    }

    @Test
    void shouldRejectExpiredToken() throws Exception {
        // 1 毫秒过期，签发后立即过期
        JwtUtil shortLived = new JwtUtil(TEST_SECRET, 1L, 1L);
        String token = shortLived.generateToken(42L, "expiredUser", "USER");

        // 等待过期
        Thread.sleep(5);

        assertThat(shortLived.validateToken(token)).isFalse();
    }

    @Test
    void shouldRejectTamperedToken() {
        String token = jwtUtil.generateToken(1L, "admin", "ADMIN");
        // 篡改最后 5 个字符
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertThat(jwtUtil.validateToken(tampered)).isFalse();
    }

    @Test
    void shouldRejectInvalidTokenFormat() {
        assertThat(jwtUtil.validateToken("not-a-jwt-token")).isFalse();
    }

    @Test
    void shouldRejectNullToken() {
        assertThat(jwtUtil.validateToken(null)).isFalse();
    }

    // ==================== refreshToken ====================

    @Test
    void shouldGenerateRefreshToken() {
        String accessToken = jwtUtil.generateToken(1L, "admin", "ADMIN");
        String refreshToken = jwtUtil.generateRefreshToken(1L, "admin", "ADMIN");

        // 两个 token 都能解析
        assertThat(jwtUtil.validateToken(accessToken)).isTrue();
        assertThat(jwtUtil.validateToken(refreshToken)).isTrue();

        // 解析结果一致
        assertThat(jwtUtil.getUserIdFromToken(refreshToken)).isEqualTo(1L);
        assertThat(jwtUtil.getRoleFromToken(refreshToken)).isEqualTo("ADMIN");
    }

    @Test
    void refreshTokenShouldHaveLongerExpiration() {
        String accessToken = jwtUtil.generateToken(1L, "u", "USER");
        String refreshToken = jwtUtil.generateRefreshToken(1L, "u", "USER");

        long accessExp = jwtUtil.getExpirationFromToken(accessToken).getTime();
        long refreshExp = jwtUtil.getExpirationFromToken(refreshToken).getTime();

        // refreshToken 过期时间应更晚
        assertThat(refreshExp).isGreaterThan(accessExp);
    }

    // ==================== token 信息提取 ====================

    @Test
    void shouldGetIssuedAtFromToken() {
        String token = jwtUtil.generateToken(99L, "testUser", "USER");

        assertThat(jwtUtil.getIssuedAtFromToken(token)).isNotNull();
        // 签发时间应该是最近几秒内
        long now = System.currentTimeMillis();
        long issued = jwtUtil.getIssuedAtFromToken(token).getTime();
        assertThat(now - issued).isLessThan(5000L); // 5 秒内
    }

    @Test
    void shouldGetRefreshExpiration() {
        assertThat(jwtUtil.getRefreshExpiration()).isEqualTo(86400000L);
    }

    // ==================== 多用户独立 ====================

    @Test
    void shouldHandleMultipleUsersIndependently() {
        String token1 = jwtUtil.generateToken(1L, "alice", "ADMIN");
        String token2 = jwtUtil.generateToken(2L, "bob", "USER");

        assertThat(jwtUtil.getUserIdFromToken(token1)).isEqualTo(1L);
        assertThat(jwtUtil.getUserIdFromToken(token2)).isEqualTo(2L);
        assertThat(jwtUtil.getUsernameFromToken(token1)).isEqualTo("alice");
        assertThat(jwtUtil.getUsernameFromToken(token2)).isEqualTo("bob");
        assertThat(jwtUtil.getRoleFromToken(token1)).isEqualTo("ADMIN");
        assertThat(jwtUtil.getRoleFromToken(token2)).isEqualTo("USER");
    }

    // ==================== 不同密钥互不识别 ====================

    @Test
    void shouldNotValidateTokenFromDifferentSecret() {
        JwtUtil jwtUtil2 = new JwtUtil(
                "AnotherSecretKey2024!@#$AnotherSecretKey2024!@",
                3600000L, 86400000L);
        String token = jwtUtil.generateToken(1L, "admin", "ADMIN");

        // 另一个密钥实例无法验证
        assertThat(jwtUtil2.validateToken(token)).isFalse();
    }
}
