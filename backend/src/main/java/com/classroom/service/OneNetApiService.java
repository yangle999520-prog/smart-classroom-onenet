package com.classroom.service;

import com.classroom.entity.SensorData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * OneNET HTTP API 数据拉取服务
 * 通过 OneNET 物模型 API 查询设备最新属性值
 *
 * 与 OneNET 平台使用完全相同的接口: /thingmodel/query-device-property
 * 采用 token 2.0 安全鉴权
 */
@Service
@ConditionalOnProperty(name = "onenet.api.enabled", havingValue = "true")
public class OneNetApiService {

    private static final Logger log = LoggerFactory.getLogger(OneNetApiService.class);

    private final SensorDataService sensorDataService;
    private final RestTemplate restTemplate;

    @Value("${onenet.api.base-url:https://iot-api.heclouds.com}")
    private String baseUrl;

    @Value("${onenet.api.product-id:}")
    private String productId;

    @Value("${onenet.api.device-name:}")
    private String deviceName;

    @Value("${onenet.api.access-key:}")
    private String accessKey;

    @Value("${onenet.api.version:2022-05-01}")
    private String version;

    public OneNetApiService(SensorDataService sensorDataService) {
        this.sensorDataService = sensorDataService;
        this.restTemplate = new RestTemplate();
    }

    @PostConstruct
    public void init() {
        log.info("======================================================");
        log.info("🚀 OneNetApiService 已启动！");
        log.info("   base-url:    {}", baseUrl);
        log.info("   product-id:  {}", productId);
        log.info("   device-name: {}", deviceName);
        log.info("   access-key:  {}", maskKey(accessKey));
        log.info("======================================================");
    }

    /**
     * 每5秒拉取一次OneNET最新数据
     */
    @Scheduled(fixedRate = 5000, initialDelay = 5000)
    public void fetchLatestData() {
        if (accessKey == null || accessKey.isEmpty() ||
            productId == null || productId.isEmpty() ||
            deviceName == null || deviceName.isEmpty()) {
            log.warn("⏭️ OneNET未配置完整，跳过拉取");
            return;
        }

        String url = String.format(
            "%s/thingmodel/query-device-property?product_id=%s&device_name=%s",
            baseUrl, productId, deviceName
        );

        try {
            log.info("🔄 正在从OneNET拉取数据...");

            String token = generateToken();
            if (token == null) return;

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("authorization", token);
            headers.set("Content-Type", "application/json");

            org.springframework.http.HttpEntity<String> entity =
                new org.springframework.http.HttpEntity<>(headers);

            org.springframework.http.ResponseEntity<Map> response =
                restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, Map.class);

            processResponse(response);

        } catch (Exception e) {
            log.warn("  OneNET请求异常: {}", e.getMessage());
        }
    }

    /**
     * 处理OneNET响应，解析数据并保存
     *
     * OneNET实际返回格式（data为数组，不是嵌套对象）:
     * {
     *   "code": 0,
     *   "msg": "succ",
     *   "data": [
     *     {"identifier": "led",  "value": false,  "data_type": "bool",  ...},
     *     {"identifier": "light","value": 1099,   "data_type": "int32", ...},
     *     {"identifier": "temp", "value": 28.2,   "data_type": "float", ...}
     *   ]
     * }
     */
    @SuppressWarnings("unchecked")
    private boolean processResponse(org.springframework.http.ResponseEntity<Map> response) {
        int statusCode = response.getStatusCodeValue();
        Map<String, Object> body = response.getBody();

        log.info("   响应状态码: {}", statusCode);
        log.info("   返回数据: {}", body != null ? body.toString() : "null");

        if (body == null) {
            log.warn("   响应体为空");
            return false;
        }

        // 检查业务状态码
        Object codeObj = body.get("code");
        if (codeObj != null && !codeObj.toString().equals("0")) {
            log.warn("   OneNET业务错误: code={}, msg={}", codeObj, body.get("msg"));
            return false;
        }

        // 解析数据
        try {
            // ⚠️ data 是数组 (ArrayList)，不是 Map
            Object dataObj = body.get("data");
            if (dataObj == null) {
                log.warn("   返回数据中 data 为空");
                return false;
            }

            List<Map<String, Object>> dataList;
            if (dataObj instanceof List) {
                dataList = (List<Map<String, Object>>) dataObj;
            } else {
                log.warn("   data 字段类型不是数组，实际类型: {}", dataObj.getClass().getName());
                return false;
            }

            if (dataList.isEmpty()) {
                log.warn("   data 数组为空（设备可能未上传数据）");
                return false;
            }

            log.info("   返回 {} 个数据点", dataList.size());

            Float temperature = null;
            Integer light = null;
            Integer ledStatus = null;

            for (Map<String, Object> item : dataList) {
                String identifier = (String) item.get("identifier");
                Object rawValue = item.get("value");
                log.info("   字段: {} = {} (类型: {})", identifier, rawValue,
                    rawValue != null ? rawValue.getClass().getSimpleName() : "null");

                if (identifier == null || rawValue == null) continue;

                switch (identifier) {
                    case "temp":    // ✅ 你的物模型字段名是 temp
                    case "temperature":
                        temperature = Float.parseFloat(rawValue.toString());
                        break;
                    case "light":
                        light = Integer.parseInt(rawValue.toString());
                        break;
                    case "led":
                        // led 的值可能是 bool 类型 false/true
                        if (rawValue instanceof Boolean) {
                            ledStatus = (Boolean) rawValue ? 1 : 0;
                        } else {
                            ledStatus = rawValue.toString().equals("true") ? 1 : 0;
                        }
                        break;
                }
            }

            if (ledStatus == null && light != null) {
                ledStatus = (light < 30) ? 1 : 0;
            }

            if (temperature != null && light != null) {
                SensorData data = new SensorData(temperature, light, ledStatus != null ? ledStatus : 0);
                sensorDataService.saveData(data);
                log.info("✅✅✅ OneNET数据已保存: {}°C, {}lux, LED={}", temperature, light, ledStatus);
                return true;
            } else {
                log.warn("   数据不完整(temp={}, light={})，跳过保存", temperature, light);
                return false;
            }

        } catch (Exception e) {
            log.error("❌ 解析OneNET数据失败: {}", e.getMessage());
            return false;
        }
    }

    // ==================== OneNET token 生成 ====================
    // 基于 OneNET 新版安全鉴权算法 (token 2.0)
    // 参考: https://blog.csdn.net/m0_75252503/article/details/140220074
    //
    // 新版算法关键点:
    // 1. res = "products/{productId}" （旧版是 products/{pid}，新版需用产品ID）
    // 2. 待签名字符串 = et + '\n' + method + '\n' + res + '\n' + version
    // 3. access_key 需先 Base64 解码再作为 HMAC 密钥
    // 4. 签名结果 sign 和 res 都需要 URL 编码

    private String generateToken() {
        try {
            if (accessKey == null || accessKey.isEmpty()) return null;

            String method = "sha1";
            String res = "products/" + productId;
            long et = System.currentTimeMillis() / 1000 + 3600;

            // ⚠️ 关键！新版签名串格式: et + \n + method + \n + res + \n + version
            String orgStr = et + "\n" + method + "\n" + res + "\n" + version;
            log.debug("   待签名字符串: {}", orgStr.replace("\n", "\\n"));

            // Base64解码access_key作为HMAC密钥
            byte[] keyBytes = Base64.getDecoder().decode(accessKey);

            // HMAC-SHA1 签名
            Mac mac = Mac.getInstance("HmacSHA1");
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "HmacSHA1");
            mac.init(keySpec);
            byte[] signBytes = mac.doFinal(orgStr.getBytes(StandardCharsets.UTF_8));
            String sign = Base64.getEncoder().encodeToString(signBytes);

            // URL编码 sign 和 res（重要！OneNET要求）
            String encodedSign = java.net.URLEncoder.encode(sign, "UTF-8");
            String encodedRes = java.net.URLEncoder.encode(res, "UTF-8");

            return String.format(
                "version=%s&res=%s&et=%d&method=%s&sign=%s",
                version, encodedRes, et, method, encodedSign
            );
        } catch (IllegalArgumentException e) {
            log.error("❌ access_key Base64解码失败: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("❌ 生成token失败: {}", e.getMessage());
            return null;
        }
    }

    private String maskKey(String key) {
        if (key == null) return "null";
        if (key.length() <= 8) return "****";
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }
}
