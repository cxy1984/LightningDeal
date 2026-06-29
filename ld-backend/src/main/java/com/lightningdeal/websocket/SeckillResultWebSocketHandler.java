package com.lightningdeal.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 秒杀结果 WebSocket 推送
 * 连接路径：/ws/seckill/result/{userId}
 * 用户抢购完成后，服务端主动推送结果
 */
@Slf4j
@Component
public class SeckillResultWebSocketHandler extends TextWebSocketHandler {

    /** userId -> WebSocketSession */
    private static final Map<Long, WebSocketSession> SESSIONS = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    public SeckillResultWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String path = session.getUri().getPath();
        // 从路径中提取 userId: /ws/seckill/result/{userId}
        String[] segments = path.split("/");
        Long userId = Long.valueOf(segments[segments.length - 1]);
        SESSIONS.put(userId, session);
        log.debug("WebSocket 连接建立 userId={}, sessionId={}", userId, session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 客户端可发送心跳消息
        String payload = message.getPayload();
        if ("ping".equals(payload)) {
            try {
                session.sendMessage(new TextMessage("pong"));
            } catch (IOException e) {
                log.error("心跳回复失败", e);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        SESSIONS.values().remove(session);
        log.debug("WebSocket 连接关闭 sessionId={}", session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket 传输异常 sessionId={}", session.getId(), exception);
        SESSIONS.values().remove(session);
    }

    /**
     * 推送秒杀结果给指定用户
     */
    public void sendResult(Long userId, Object result) {
        WebSocketSession session = SESSIONS.get(userId);
        if (session != null && session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(result);
                session.sendMessage(new TextMessage(json));
                log.debug("推送秒杀结果 userId={}, data={}", userId, json);
            } catch (IOException e) {
                log.error("推送秒杀结果失败 userId={}", userId, e);
            }
        }
    }

    /**
     * 广播消息给所有连接的用户
     */
    public void broadcast(Object message) {
        String json;
        try {
            json = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("广播消息序列化失败", e);
            return;
        }
        SESSIONS.values().forEach(session -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(json));
                } catch (IOException e) {
                    log.error("广播消息发送失败", e);
                }
            }
        });
    }
}
