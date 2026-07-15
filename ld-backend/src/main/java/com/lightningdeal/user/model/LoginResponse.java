package com.lightningdeal.user.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录返回
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "登录返回")
public class LoginResponse {

    @Schema(description = "访问令牌（短期，7天）")
    private String accessToken;

    @Schema(description = "刷新令牌（长期，30天）")
    private String refreshToken;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;
}
