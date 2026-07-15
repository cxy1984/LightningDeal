package com.lightningdeal.user.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lightningdeal.common.exception.BizException;
import com.lightningdeal.common.service.TokenBlacklistService;
import com.lightningdeal.config.JwtUtil;
import com.lightningdeal.user.entity.User;
import com.lightningdeal.user.mapper.UserMapper;
import com.lightningdeal.user.model.LoginRequest;
import com.lightningdeal.user.model.LoginResponse;
import com.lightningdeal.user.model.RegisterRequest;
import com.lightningdeal.user.model.UpdatePasswordRequest;
import com.lightningdeal.user.model.UpdateProfileRequest;
import com.lightningdeal.user.model.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService blacklistService;

    @Override
    public UserVO register(RegisterRequest request) {
        // 检查用户名是否已存在
        if (lambdaQuery().eq(User::getUsername, request.getUsername()).count() > 0) {
            throw new BizException(400, "用户名已存在");
        }

        // 检查手机号是否已存在
        if (lambdaQuery().eq(User::getPhone, request.getPhone()).count() > 0) {
            throw new BizException(400, "手机号已注册");
        }

        // 创建用户
        User user = new User();
        BeanUtils.copyProperties(request, user);
        if (user.getNickname() == null) {
            user.setNickname(request.getUsername());
        }
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        save(user);

        log.info("用户注册成功 userId={}, username={}", user.getId(), user.getUsername());
        return toVO(user);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = lambdaQuery().eq(User::getUsername, request.getUsername()).one();
        if (user == null) {
            throw new BizException(401, "用户名或密码错误");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BizException(401, "用户名或密码错误");
        }

        String accessToken = jwtUtil.generateToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());
        log.info("用户登录成功 userId={}, username={}", user.getId(), user.getUsername());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .username(user.getUsername())
                .build();
    }

    @Override
    public UserVO getUserInfo(Long userId) {
        User user = getById(userId);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        return toVO(user);
    }

    @Override
    public UserVO updateProfile(Long userId, UpdateProfileRequest request) {
        User user = getById(userId);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        user.setNickname(request.getNickname());
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        updateById(user);
        log.info("用户信息更新 userId={}", userId);
        return toVO(user);
    }

    @Override
    public void updatePassword(Long userId, UpdatePasswordRequest request) {
        User user = getById(userId);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BizException(400, "原密码错误");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        updateById(user);
        // 修改密码后使该用户所有 refreshToken 失效
        blacklistService.invalidateUserTokens(userId, 30 * 24 * 3600);
        log.info("密码修改成功 userId={}，旧 refreshToken 已失效", userId);
    }

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
