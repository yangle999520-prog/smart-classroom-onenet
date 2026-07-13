package com.classroom.service;

import com.classroom.entity.SensorData;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * MQTT回调处理器
 * 接收OneNET平台推送的传感器数据，自动存库
 */
@Service
public class MqttCallbackHandler implements MqttCallback {

    private static final Logger log = LoggerFactory.getLogger(MqttCallbackHandler.class);

    private final SensorDataService sensorDataService;

    public MqttCallbackHandler(SensorDataService sensorDataService) {
        this.sensorDataService = sensorDataService;
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.warn("MQTT连接断开: {}", cause.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload());
        log.info("收到OneNET数据 - 主题: {}, 内容: {}", topic, payload);

        try {
            // 解析OneNET MQTT JSON格式（两种常见格式，根据实际情况调整）
            // 格式1: {"temperature": 25.6, "light": 500, "led": 0}
            // 格式2: {"id":"xxx","params":{"temperature":25.6,"light":500,"led":0}}

            String json = payload;

            // 判断是否为OneNET标准物模型格式（嵌套params）
            if (json.contains("\"params\"")) {
                // 提取params内的JSON
                int start = json.indexOf("\"params\":") + 9;
                int end = json.lastIndexOf("}");
                json = json.substring(start, end + 1);
            }

            // 解析各字段
            Float temperature = extractFloat(json, "temperature");
            Integer light = extractInt(json, "light");
            Integer led = extractInt(json, "led");

            if (temperature != null && light != null) {
                if (led == null) {
                    led = (light < 30) ? 1 : 0; // 根据阈值自动判断
                }

                SensorData data = new SensorData(temperature, light, led);
                sensorDataService.saveData(data);
                log.info("✅ OneNET数据已保存: {}°C, {}lux, LED={}", temperature, light, led);
            }
        } catch (Exception e) {
            log.error("解析OneNET消息失败: {}", e.getMessage());
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // 消息发送完成回调
    }

    // ==================== 简单JSON字段提取 ====================
    private Float extractFloat(String json, String key) {
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx == -1) return null;
        int start = idx + search.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        if (end == -1) return null;
        try {
            return Float.parseFloat(json.substring(start, end).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer extractInt(String json, String key) {
        Float val = extractFloat(json, key);
        return val != null ? val.intValue() : null;
    }
}
