package com.classroom.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置
 * 注册 /ws/sensor 端点，用于实时推送传感器数据到前端
 *
 * 前端连接方式: new WebSocket('ws://host:8080/ws/sensor')
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final SensorWebSocketHandler sensorWebSocketHandler;

    public WebSocketConfig(SensorWebSocketHandler sensorWebSocketHandler) {
        this.sensorWebSocketHandler = sensorWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(sensorWebSocketHandler, "/ws/sensor")
                .setAllowedOrigins("*");  // 允许跨域
    }
}
