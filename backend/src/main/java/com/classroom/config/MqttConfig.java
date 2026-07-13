package com.classroom.config;

import com.classroom.service.MqttCallbackHandler;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * OneNET MQTT 客户端配置
 * 用于订阅OneNET平台下发的控制指令
 */
@Configuration
public class MqttConfig {

    private static final Logger log = LoggerFactory.getLogger(MqttConfig.class);

    @Value("${mqtt.broker.server:}")
    private String brokerServer;

    @Value("${mqtt.broker.port:1883}")
    private int brokerPort;

    @Value("${mqtt.broker.client-id:}")
    private String clientId;

    @Value("${mqtt.broker.username:}")
    private String username;

    @Value("${mqtt.broker.password:}")
    private String password;

    @Value("${mqtt.broker.topic:}")
    private String topic;

    @Value("${mqtt.broker.qos:1}")
    private int qos;

    @Bean(destroyMethod = "close")
    @Conditional(MqttCondition.class)
    public MqttClient mqttClient(MqttCallbackHandler callbackHandler) throws MqttException {
        String serverUri = String.format("tcp://%s:%d", brokerServer, brokerPort);
        MqttClient client = new MqttClient(serverUri, clientId, new MemoryPersistence());

        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        options.setConnectionTimeout(30);
        options.setKeepAliveInterval(60);

        if (username != null && !username.isEmpty()) {
            options.setUserName(username);
        }
        if (password != null && !password.isEmpty()) {
            options.setPassword(password.toCharArray());
        }

        try {
            client.setCallback(callbackHandler);
            client.connect(options);
            log.info("MQTT连接成功: {}", serverUri);

            if (topic != null && !topic.isEmpty()) {
                client.subscribe(topic, qos);
                log.info("MQTT订阅主题: {}", topic);
            }
        } catch (MqttException e) {
            log.error("MQTT连接失败: {}", e.getMessage());
            log.warn("系统将以HTTP模式运行，等待MQTT配置后重启即可启用MQTT");
        }

        return client;
    }
}
