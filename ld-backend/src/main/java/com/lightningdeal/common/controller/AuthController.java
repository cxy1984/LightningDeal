package com.lightningdeal.common.controller;

import com.lightningdeal.common.response.R;
import com.lightningdeal.common.service.TokenBlacklistService;
import com.lightningdeal.config.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Token 刷新控制器
 */
@Slf4j
@Tag(name = "Token 管理", description = "Token 刷新与失效")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtil jwtUtil;
    private final TokenBlacklistService blacklistService;

    @Operation(summary = "刷新 accessToken")
    @PostMapping("/refresh")
    public R<Map<String, String>> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isEmpty()) {
            return R.fail(400, "refreshToken 不能为空");
        }

        // 1. 校验 refreshToken 是否过期
        if (!jwtUtil.validateToken(refreshToken)) {
            return R.fail(401, "refreshToken 已过期，请重新登录");
        }

        // 2. 检查黑名单
        if (blacklistService.isBlacklisted(refreshToken)) {
            return R.fail(401, "refreshToken 已被吊销，请重新登录");
        }

        // 3. 检查用户级黑名单（修改密码时使所有 token 失效）
        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        String userVersion = blacklistService.getUserTokenVersion(userId);
        if (userVersion != null) {
            // 从 token 中解析签发时间
            Date iat = jwtUtil.getIssuedAtFromToken(refreshToken);
            // 黑名单版本时间戳 > token 签发时间 → token 应失效
            long blacklistTime = Long.parseLong(userVersion);
            if (iat.getTime() < blacklistTime) {
                return R.fail(401, "refreshToken 已被吊销，请重新登录");
            }
        }

        // 4. 签发新的 accessToken + refreshToken（轮换）
        String username = jwtUtil.getUsernameFromToken(refreshToken);
        String newAccessToken = jwtUtil.generateToken(userId, username);
        String newRefreshToken = jwtUtil.generateRefreshToken(userId, username);

        log.info("Token 刷新成功 userId={}", userId);

        Map<String, String> result = new HashMap<>();
        result.put("accessToken", newAccessToken);
        result.put("refreshToken", newRefreshToken);
        return R.ok(result);
    }

    @Operation(summary = "主动失效 refreshToken（登出）")
    @PostMapping("/logout")
    public R<String> logout(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken != null && !refreshToken.isEmpty()) {
            try {
                Date exp = jwtUtil.getExpirationFromToken(refreshToken);
                long ttl = (exp.getTime() - System.currentTimeMillis()) / 1000;
                if (ttl > 0) {
                    blacklistService.addToBlacklist(refreshToken, ttl);
                }
            } catch (Exception e) {
                // token 已过期，无需加入黑名单
            }
        }
        return R.ok("已登出");
    }
}
