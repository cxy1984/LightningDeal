package com.lightningdeal.config;

import com.lightningdeal.user.entity.User;
import com.lightningdeal.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 启动时初始化测试用户（确保密码正确）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initUser("admin", "123456", "13800138000", "管理员");
        initUser("testuser", "123456", "13900139000", "测试用户");
    }

    private void initUser(String username, String rawPassword, String phone, String nickname) {
        try {
            User user = userService.lambdaQuery().eq(User::getUsername, username).one();
            if (user == null) {
                // 如果用户不存在则创建
                user = new User();
                user.setUsername(username);
                user.setPassword(passwordEncoder.encode(rawPassword));
                user.setPhone(phone);
                user.setNickname(nickname);
                userService.save(user);
                log.info("初始化用户: {}", username);
            } else if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
                // 密码不匹配则重新加密
                user.setPassword(passwordEncoder.encode(rawPassword));
                userService.updateById(user);
                log.info("已重置用户密码: {}", username);
            }
        } catch (Exception e) {
            log.warn("初始化用户失败（可忽略）: {}", e.getMessage());
        }
    }
}
