package com.classroom.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket 处理器
 * 管理前端仪表盘的 WebSocket 连接，实时推送传感器数据
 *
 * 端点路径: ws://host/ws/sensor
 */
@Component
public class SensorWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(SensorWebSocketHandler.class);

    /** 所有已连接的 WebSocket 会话（线程安全） */
    private static final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("📡 WebSocket 客户端已连接: {}, 当前在线: {}", session.getId(), sessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("📡 WebSocket 客户端断开: {}, 当前在线: {}", session.getId(), sessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 服务端暂不需要处理客户端发来的消息
    }

    /**
     * 向所有连接的 WebSocket 客户端广播消息
     *
     * @param message 要推送的 JSON 字符串
     */
    public static void broadcast(String message) {
        if (sessions.isEmpty()) {
            return;
        }

        TextMessage textMessage = new TextMessage(message);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    synchronized (session) {
                        session.sendMessage(textMessage);
                    }
                } catch (IOException e) {
                    log.warn("  WebSocket 发送失败 ({}): {}", session.getId(), e.getMessage());
                }
            }
        }
    }

    /** 当前在线客户端数 */
    public static int getOnlineCount() {
        return sessions.size();
    }
}
