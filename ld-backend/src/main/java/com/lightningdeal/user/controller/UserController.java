package com.lightningdeal.user.controller;

import com.lightningdeal.common.response.R;
import com.lightningdeal.user.model.LoginRequest;
import com.lightningdeal.user.model.RegisterRequest;
import com.lightningdeal.user.model.UserVO;
import com.lightningdeal.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 用户控制器
 */
@Tag(name = "用户管理", description = "注册、登录、获取用户信息")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public R<UserVO> register(@Valid @RequestBody RegisterRequest request) {
        return R.ok(userService.register(request));
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public R<String> login(@Valid @RequestBody LoginRequest request) {
        String token = userService.login(request);
        return R.ok("登录成功", token);
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/info")
    public R<UserVO> getUserInfo(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return R.ok(userService.getUserInfo(userId));
    }
}
