package com.classroom.onenet.pulsar.consumer;

import com.alibaba.fastjson2.JSONObject;
import com.classroom.config.SensorWebSocketHandler;
import com.classroom.entity.SensorData;
import com.classroom.onenet.pulsar.auth.IoTAuthentication;
import com.classroom.onenet.pulsar.config.IoTConfig;
import com.classroom.onenet.pulsar.entity.IoTMessage;
import com.classroom.onenet.pulsar.util.AESBase64Utils;
import com.classroom.service.SensorDataService;
import org.apache.pulsar.client.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * OneNET Pulsar 服务端订阅消费者
 *
 * 替代原有的 OneNetApiService（HTTP 轮询），改用 OneNET 的服务端订阅（Pulsar）接收设备数据。
 * 当设备上报数据时，OneNET 平台主动推送到 Pulsar Topic，本服务实时接收并存入数据库。
 *
 * 原理：使用 iot-pulsar-sdk-java-3.0.1 的鉴权和连接方式，
 * 通过 Pulsar 协议连接到 OneNET 平台的消息队列，订阅设备事件。
 *
 * 配置参数：
 *   onenet.pulsar.access-id      - 消费组 ID（在 OneNET 平台创建服务端订阅时获得）
 *   onenet.pulsar.secret-key     - 消费组密钥
 *   onenet.pulsar.subscription   - 订阅名称（通常为 {消费组ID}-sub）
 *
 * 启用条件：onenet.pulsar.enabled=true
 */
@Service
@ConditionalOnProperty(name = "onenet.pulsar.enabled", havingValue = "true")
public class OneNetPulsarSubscriber implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(OneNetPulsarSubscriber.class);

    private final SensorDataService sensorDataService;

    /** Broker 地址（固定使用 OneNET 北方节点） */
    private static final String BROKER_URL = IoTConfig.brokerSSLServerUrl;

    /** Topic 格式：{accessId}/iot/event */
    private static final String TOPIC_FORMAT = "%s" + IoTConfig.DEFAULT_TOPIC_SUFFIX;

    /** 订阅类型：故障转移 */
    private static final SubscriptionType SUB_TYPE = SubscriptionType.Failover;

    @Value("${onenet.pulsar.access-id:}")
    private String iotAccessId;

    @Value("${onenet.pulsar.secret-key:}")
    private String iotSecretKey;

    @Value("${onenet.pulsar.subscription:}")
    private String subscriptionName;

    /** Pulsar 客户端实例，在销毁时关闭 */
    private PulsarClient pulsarClient;

    /** 消费者线程，防止 JVM 退出 */
    private Thread consumerThread;

    public OneNetPulsarSubscriber(SensorDataService sensorDataService) {
        this.sensorDataService = sensorDataService;
    }

    @PostConstruct
    public void init() {
        log.info("================================================================");
        log.info("  OneNet Pulsar 服务端订阅准备启动");
        log.info("  Broker:       {}", BROKER_URL);
        log.info("  AccessId:     {}", iotAccessId);
        log.info("  SecretKey:    {}", maskKey(iotSecretKey));
        log.info("  Subscription: {}", subscriptionName);
        log.info("================================================================");

        if (iotAccessId == null || iotAccessId.isEmpty()) {
            log.error("❌ onenet.pulsar.access-id 未配置，服务端订阅启动失败");
            return;
        }
        if (iotSecretKey == null || iotSecretKey.isEmpty()) {
            log.error("❌ onenet.pulsar.secret-key 未配置，服务端订阅启动失败");
            return;
        }
        if (subscriptionName == null || subscriptionName.isEmpty()) {
            log.error("❌ onenet.pulsar.subscription 未配置，服务端订阅启动失败");
            return;
        }

        // 在独立线程中启动 Pulsar 消费者（避免阻塞 Spring 容器启动）
        consumerThread = new Thread(this::startConsumer, "onenet-pulsar-consumer");
        consumerThread.setDaemon(true);
        consumerThread.start();

        log.info("✅ OneNet Pulsar 服务端订阅线程已启动");
    }

    /**
     * 启动 Pulsar 消费者（在独立线程中运行）
     * 持续监听 OneNET 平台推送的设备事件消息
     */
    private void startConsumer() {
        try {
            // 1. 创建 Pulsar 客户端
            pulsarClient = PulsarClient.builder()
                    .serviceUrl(BROKER_URL)
                    .allowTlsInsecureConnection(true)
                    .authentication(new IoTAuthentication(iotAccessId, iotSecretKey))
                    .build();

            // 2. 创建消费者
            String topic = String.format(TOPIC_FORMAT, iotAccessId);
            Consumer<String> consumer = pulsarClient.newConsumer(Schema.STRING)
                    .topic(topic)
                    .subscriptionName(subscriptionName)
                    .subscriptionType(SUB_TYPE)
                    .autoUpdatePartitions(Boolean.FALSE)
                    .subscribe();

            log.info("✅ Pulsar 消费者已连接到 Topic: {}", topic);

            // 3. 消息处理循环
            while (!Thread.currentThread().isInterrupted()) {
                Message<String> message = null;
                try {
                    message = consumer.receive();
                    handleMessage(message);
                } catch (Throwable t) {
                    log.error("❌ 消息处理异常: {}", t.toString());
                } finally {
                    // 无论处理成功还是失败，都 ACK 确认消息
                    // 避免消息积压导致过期
                    if (message != null) {
                        try {
                            consumer.acknowledge(message);
                        } catch (Exception e) {
                            log.warn("  ACK 消息失败: {}", e.getMessage());
                        }
                    }
                }
            }

            consumer.close();

        } catch (PulsarClientException e) {
            log.error("❌ Pulsar 客户端异常: {}", e.getMessage());
        } catch (Exception e) {
            log.error("❌ 消费者意外终止: {}", e.getMessage());
        }
    }

    /**
     * 处理 OneNET 推送的消息
     *
     * 消息格式（外层 JSON）：
     * {
     *   "data": "<AES加密的Base64数据>",
     *   "superMsg": 0,
     *   "pv": "1.0",
     *   "t": 1695000000000,
     *   "sign": "..."
     * }
     *
     * 解密后的原始设备数据示例（取决于设备上报的物模型格式）：
     * {"temp":28.2,"light":1099,"led":false,"mode":0}
     * 或
     * [{"identifier":"temp","value":28.2}, ...]
     */
    /** Pulsar 端到端延迟统计（滑动窗口） */
    private long totalPulsarLatencyMs = 0;
    private long totalProcessLatencyMs = 0;
    private long msgCount = 0;

    private void handleMessage(Message<String> message) {
        long receiveTime = System.currentTimeMillis();
        MessageId msgId = message.getMessageId();
        long pulsarPublishTime = message.getPublishTime();
        String payload = message.getValue();

        log.info("📥 收到 OneNET Pulsar 消息: msgId={}", msgId);

        try {
            // 1. 解析外层 IoTMessage
            IoTMessage iotMessage = JSONObject.parseObject(payload, IoTMessage.class);

            if (iotMessage == null || iotMessage.getData() == null || iotMessage.getData().isEmpty()) {
                log.warn("    消息体为空或 data 字段为空，跳过");
                return;
            }

            // 链路①：设备上报 → OneNET Pulsar 发布时间
            long deviceTs = iotMessage.getT() != null ? iotMessage.getT() : 0;
            long deviceToPulsar = deviceTs > 0 ? pulsarPublishTime - deviceTs : -1;

            // ②：Pulsar 发布时间 → 后端接收时间（Pulsar 传输延迟）
            long pulsarLatency = receiveTime - pulsarPublishTime;
            // ③：设备上报 → 后端接收时间（总端到端延迟）
            long totalLatency = receiveTime - (deviceTs > 0 ? deviceTs : pulsarPublishTime);

            log.info("    ⏱ 延迟统计:");
            if (deviceTs > 0) {
                log.info("       ① 设备→OneNET(上报耗时): {}ms", deviceToPulsar);
            }
            log.info("       ② OneNET Pulsar→后端(传输延迟): {}ms", pulsarLatency);
            log.info("       ③ 设备→后端(总端到端延迟): {}ms", totalLatency);

            long processStart = System.currentTimeMillis();

            // 2. 解密数据（AES-128，密钥为 secretKey 的第 8~24 位）
            String originalMsg = AESBase64Utils.decrypt(
                    iotMessage.getData(),
                    iotSecretKey.substring(8, 24)
            );

            long decryptEnd = System.currentTimeMillis();
            log.info("    ⏱ 解密耗时: {}ms", decryptEnd - processStart);
            log.info("    解密后原始数据: {}", originalMsg);

            // 3. 解析设备数据并保存
            parseAndSaveDeviceData(originalMsg);

            long processEnd = System.currentTimeMillis();

            // ④：后端处理延迟（解密 + 解析 + 存库）
            long processLatency = processEnd - processStart;
            log.info("    ⏱ ④ 后端处理耗时(解密+解析+存库): {}ms", processLatency);

            // 更新滚动平均统计
            msgCount++;
            totalPulsarLatencyMs += pulsarLatency;
            totalProcessLatencyMs += processLatency;
            if (msgCount % 10 == 0) {
                log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                log.info("  📊 滚动平均延迟 (最近{}条消息):", msgCount);
                log.info("     Pulsar传输延迟均: {}ms", totalPulsarLatencyMs / msgCount);
                log.info("     后端处理延迟均:   {}ms", totalProcessLatencyMs / msgCount);
                log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            }

        } catch (Exception e) {
            log.error("❌ 消息解析失败: {}", e.getMessage());
        }
    }

    /**
     * 解析设备上报的物模型数据并存入数据库
     *
     * 解密后的数据是 OneNET 标准物模型格式（服务端订阅推送）：
     * {
     *   "msgType": "thingProperty",
     *   "subData": {
     *     "deviceId": "2615989324",
     *     "deviceName": "D1",
     *     "params": {
     *       "temp":  { "time": ..., "value": 26.5 },
     *       "light": { "time": ..., "value": 52 },
     *       "led":   { "time": ..., "value": true },
     *       "mode":  { "time": ..., "value": false }
     *     },
     *     "productId": "..."
     *   }
     * }
     *
     * 也兼容之前的格式：
     * - 物模型数组格式：[{"identifier":"temp","value":28.2}, ...]
     * - 键值对格式：  {"temp":28.2,"light":1099}
     */
    private void parseAndSaveDeviceData(String jsonData) {
        if (jsonData == null || jsonData.trim().isEmpty()) {
            log.warn("    解密数据为空，跳过保存");
            return;
        }

        Float temperature = null;
        Integer light = null;
        Integer ledStatus = null;
        Integer mode = 0;  // 默认自动模式

        try {
            Object parsed = JSONObject.parse(jsonData);

            if (parsed instanceof com.alibaba.fastjson2.JSONArray) {
                // 格式1：物模型数组 [{"identifier":"temp","value":28.2}, ...]
                parseArrayFormat((com.alibaba.fastjson2.JSONArray) parsed);

            } else if (parsed instanceof com.alibaba.fastjson2.JSONObject) {
                com.alibaba.fastjson2.JSONObject obj = (com.alibaba.fastjson2.JSONObject) parsed;

                // 格式3（最新）：OneNET 服务端订阅标准格式
                // 特征：包含 msgType 和 subData.params
                if (obj.containsKey("msgType") && obj.containsKey("subData")) {
                    parseOneNetStandardFormat(obj);
                } else {
                    // 格式2：简单键值对 {"temp":28.2,"light":1099}
                    parseSimpleKeyValueFormat(obj);
                }
            } else {
                log.warn("    无法识别的数据格式: {}", parsed != null ? parsed.getClass().getName() : "null");
                return;
            }

        } catch (Exception e) {
            log.error("❌ 解析设备数据失败: {}", e.getMessage());
        }
    }

    /**
     * 解析 OneNET 标准物模型格式（服务端订阅推送）
     *
     * 格式：
     * {
     *   "msgType": "thingProperty",
     *   "subData": {
     *     "params": {
     *       "temp":  { "value": 26.5 },
     *       "light": { "value": 52 },
     *       "led":   { "value": true },
     *       "mode":  { "value": false }
     *     }
     *   }
     * }
     */
    private void parseOneNetStandardFormat(com.alibaba.fastjson2.JSONObject root) {
        com.alibaba.fastjson2.JSONObject subData = root.getJSONObject("subData");
        if (subData == null) {
            log.warn("    标准格式缺少 subData 字段");
            return;
        }

        com.alibaba.fastjson2.JSONObject params = subData.getJSONObject("params");
        if (params == null) {
            log.warn("    标准格式缺少 subData.params 字段");
            return;
        }

        log.info("    设备: {}, 产品: {}, 解析 params 中...",
                subData.getString("deviceName"), subData.getString("productId"));

        Float temperature = null;
        Integer light = null;
        Integer ledStatus = null;
        Integer mode = 0;

        // 遍历每个参数字段，提取 value
        for (String key : params.keySet()) {
            Object valueObj = params.get(key);
            if (valueObj == null) continue;

            // 每个 param 是 {"time": ..., "value": ...} 格式
            Object rawValue = null;
            if (valueObj instanceof com.alibaba.fastjson2.JSONObject) {
                rawValue = ((com.alibaba.fastjson2.JSONObject) valueObj).get("value");
            } else {
                rawValue = valueObj;  // 也可能是直接值
            }
            if (rawValue == null) continue;

            switch (key) {
                case "temp":
                case "temperature":
                    try {
                        temperature = Float.parseFloat(rawValue.toString());
                    } catch (NumberFormatException e) {
                        log.warn("      温度值解析失败: {}", rawValue);
                    }
                    break;
                case "light":
                    try {
                        light = Integer.parseInt(rawValue.toString());
                    } catch (NumberFormatException e) {
                        log.warn("      光照值解析失败: {}", rawValue);
                    }
                    break;
                case "led":
                    ledStatus = parseBooleanValue(rawValue) ? 1 : 0;
                    break;
                case "mode":
                    mode = parseBooleanValue(rawValue) ? 1 : 0;
                    break;
            }
        }

        saveIfComplete(temperature, light, ledStatus, mode);
    }

    /**
     * 解析物模型数组格式
     * [{"identifier":"temp","value":28.2}, {"identifier":"light","value":1099}]
     */
    private void parseArrayFormat(com.alibaba.fastjson2.JSONArray arr) {
        Float temperature = null;
        Integer light = null;
        Integer ledStatus = null;
        Integer mode = 0;

        for (int i = 0; i < arr.size(); i++) {
            JSONObject item = arr.getJSONObject(i);
            if (item == null) continue;

            String identifier = item.getString("identifier");
            Object value = item.get("value");
            if (identifier == null || value == null) continue;

            switch (identifier) {
                case "temp":
                case "temperature":
                    temperature = Float.parseFloat(value.toString());
                    break;
                case "light":
                    light = Integer.parseInt(value.toString());
                    break;
                case "led":
                    ledStatus = parseBooleanValue(value) ? 1 : 0;
                    break;
                case "mode":
                    mode = parseBooleanValue(value) ? 1 : 0;
                    break;
            }
        }

        saveIfComplete(temperature, light, ledStatus, mode);
    }

    /**
     * 解析简单键值对格式
     * {"temp":28.2,"light":1099,"led":false,"mode":0}
     */
    private void parseSimpleKeyValueFormat(com.alibaba.fastjson2.JSONObject obj) {
        Float temperature = null;
        Integer light = null;
        Integer ledStatus = null;
        Integer mode = 0;

        if (obj.containsKey("temp") || obj.containsKey("temperature")) {
            temperature = Float.parseFloat(
                    obj.get(obj.containsKey("temp") ? "temp" : "temperature").toString()
            );
        }
        if (obj.containsKey("light")) {
            light = Integer.parseInt(obj.get("light").toString());
        }
        if (obj.containsKey("led")) {
            ledStatus = parseBooleanValue(obj.get("led")) ? 1 : 0;
        }
        if (obj.containsKey("mode")) {
            mode = parseBooleanValue(obj.get("mode")) ? 1 : 0;
        }

        saveIfComplete(temperature, light, ledStatus, mode);
    }

    /**
     * 如果数据完整则保存，否则打印警告
     */
    private void saveIfComplete(Float temperature, Integer light, Integer ledStatus, Integer mode) {
        // 如果 LED 状态未直接上报，根据光照值推断
        if (ledStatus == null && light != null) {
            ledStatus = (light < 30) ? 1 : 0;
        }

        if (temperature != null && light != null) {
            SensorData data = new SensorData(temperature, light,
                    ledStatus != null ? ledStatus : 0, mode);
            sensorDataService.saveData(data);
            log.info("✅✅✅ Pulsar 数据已保存: {}°C, {}lux, LED={}, mode={}",
                    temperature, light, ledStatus, mode);

            // 通过 WebSocket 实时推送给所有已连接的前端客户端
            try {
                com.alibaba.fastjson2.JSONObject wsMsg = new com.alibaba.fastjson2.JSONObject();
                wsMsg.put("type", "sensor_update");
                wsMsg.put("temperature", temperature);
                wsMsg.put("light", light);
                wsMsg.put("ledStatus", ledStatus != null ? ledStatus : 0);
                wsMsg.put("mode", mode);
                wsMsg.put("createTime", data.getCreateTime() != null ?
                        data.getCreateTime().toString() : java.time.LocalDateTime.now().toString());
                SensorWebSocketHandler.broadcast(wsMsg.toString());
            } catch (Exception e) {
                log.warn("  WebSocket 推送失败: {}", e.getMessage());
            }
        } else {
            log.warn("    数据不完整(temperature={}, light={})，跳过保存",
                    temperature, light);
        }
    }

    /**
     * 将布尔值（Boolean / String / Number）解析为 boolean
     */
    private boolean parseBooleanValue(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        String str = value.toString().toLowerCase().trim();
        return "true".equals(str) || "1".equals(str);
    }

    /**
     * Bean 销毁时关闭 Pulsar 客户端
     */
    @Override
    public void destroy() {
        log.info("正在关闭 OneNET Pulsar 消费者...");
        if (pulsarClient != null) {
            try {
                pulsarClient.close();
                log.info("  Pulsar 客户端已关闭");
            } catch (Exception e) {
                log.warn("  关闭 Pulsar 客户端异常: {}", e.getMessage());
            }
        }
    }

    /** 脱敏显示密钥 */
    private String maskKey(String key) {
        if (key == null) return "null";
        if (key.length() <= 8) return "****";
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }
}
