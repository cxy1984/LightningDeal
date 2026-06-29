package com.lightningdeal.config;

import com.lightningdeal.websocket.DashboardWebSocketHandler;
import com.lightningdeal.websocket.SeckillResultWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    public static final String SECKILL_RESULT_PATH = "/ws/seckill/result";
    public static final String DASHBOARD_PATH = "/ws/dashboard";

    private final SeckillResultWebSocketHandler seckillResultHandler;
    private final DashboardWebSocketHandler dashboardHandler;

    public WebSocketConfig(SeckillResultWebSocketHandler seckillResultHandler,
                           DashboardWebSocketHandler dashboardHandler) {
        this.seckillResultHandler = seckillResultHandler;
        this.dashboardHandler = dashboardHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(seckillResultHandler, SECKILL_RESULT_PATH + "/{userId}")
                .setAllowedOrigins("*");
        registry.addHandler(dashboardHandler, DASHBOARD_PATH)
                .setAllowedOrigins("*");
    }
}
