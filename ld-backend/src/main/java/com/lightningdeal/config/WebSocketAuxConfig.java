package com.lightningdeal.config;

import com.lightningdeal.websocket.DashboardWebSocketHandler;
import com.lightningdeal.websocket.SeckillResultWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/**
 * WebSocket 辅助配置
 */
@Configuration
public class WebSocketAuxConfig {

    @Bean
    public ServletServerContainerFactoryBean webSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(8192);
        container.setMaxBinaryMessageBufferSize(8192);
        container.setMaxSessionIdleTimeout(600000L); // 10min
        return container;
    }
}
