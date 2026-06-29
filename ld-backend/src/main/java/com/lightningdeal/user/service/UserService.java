package com.lightningdeal.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lightningdeal.user.entity.User;
import com.lightningdeal.user.model.LoginRequest;
import com.lightningdeal.user.model.RegisterRequest;
import com.lightningdeal.user.model.UserVO;

/**
 * 用户服务
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     */
    UserVO register(RegisterRequest request);

    /**
     * 用户登录，返回 Token
     */
    String login(LoginRequest request);

    /**
     * 根据用户ID获取用户信息
     */
    UserVO getUserInfo(Long userId);
}
