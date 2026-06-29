package com.lightningdeal.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 数据大屏 WebSocket
 * 连接路径：/ws/dashboard
 * 实时推送 QPS、抢购流水、排行榜等数据
 */
@Slf4j
@Component
public class DashboardWebSocketHandler extends TextWebSocketHandler {

    /** 所有大屏连接的会话 */
    private static final Set<WebSocketSession> SESSIONS = new CopyOnWriteArraySet<>();

    private final ObjectMapper objectMapper;

    public DashboardWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        SESSIONS.add(session);
        log.debug("大屏 WebSocket 连接建立 sessionId={}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        if ("ping".equals(message.getPayload())) {
            try {
                session.sendMessage(new TextMessage("pong"));
            } catch (IOException e) {
                log.error("大屏心跳回复失败", e);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        SESSIONS.remove(session);
        log.debug("大屏 WebSocket 连接关闭 sessionId={}", session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("大屏 WebSocket 传输异常", exception);
        SESSIONS.remove(session);
    }

    /**
     * 推送大屏数据
     */
    public void pushDashboardData(Object data) {
        String json;
        try {
            json = objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.error("大屏数据序列化失败", e);
            return;
        }
        SESSIONS.forEach(session -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(json));
                } catch (IOException e) {
                    log.error("大屏数据推送失败", e);
                }
            }
        });
    }
}
