package com.classroom.onenet.pulsar.config;

/**
 * OneNET Pulsar 服务端订阅配置常量
 * 对应原 SDK 中的 com.chinamobile.iot.pulsar.config.IoTConfig
 */
public class IoTConfig {

    /** OneNET Pulsar Broker SSL 地址（北方节点） */
    public static final String brokerSSLServerUrl = "pulsar+ssl://iot-north-mq.heclouds.com:6651/";

    /** 默认 Topic 后缀 */
    public static final String DEFAULT_TOPIC_SUFFIX = "/iot/event";
}
