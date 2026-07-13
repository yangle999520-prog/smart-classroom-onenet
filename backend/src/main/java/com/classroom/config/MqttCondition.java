package com.classroom.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * MQTT配置启用条件：仅当broker.server配置不为空时才启用MQTT客户端
 */
public class MqttCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String server = context.getEnvironment().getProperty("mqtt.broker.server");
        return server != null && !server.isEmpty();
    }
}
