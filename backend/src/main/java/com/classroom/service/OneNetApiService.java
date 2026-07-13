package com.classroom.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * OneNET HTTP API 服务
 *
 * ⚠️ 注意：数据获取已从 HTTP 轮询切换为 Pulsar 服务端订阅
 * 请参见 {@link com.classroom.onenet.pulsar.consumer.OneNetPulsarSubscriber}
 *
 * 本服务仅保留设备在线状态查询功能
 * 通过 OneNET /device/detail API 查询设备是否在线
 */
@Service
@ConditionalOnProperty(name = "onenet.api.enabled", havingValue = "true")
public class OneNetApiService {

    private static final Logger log = LoggerFactory.getLogger(OneNetApiService.class);

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

    public OneNetApiService() {
        this.restTemplate = new RestTemplate();
    }

    @PostConstruct
    public void init() {
        log.info("======================================================");
        log.info("  OneNetApiService 已启动（仅设备状态查询）");
        log.info("  数据获取已切换至 Pulsar 服务端订阅");
        log.info("  base-url:    {}", baseUrl);
        log.info("  product-id:  {}", productId);
        log.info("  device-name: {}", deviceName);
        log.info("======================================================");
    }

    // ==================== 设备在线状态检测 ====================

    /**
     * 调用 OneNET 平台 /device/detail API 查询设备在线状态
     *
     * 响应中 data.status 含义：
     *   0 = 离线
     *   1 = 在线
     *   2 = 未激活
     *
     * @return Map: online(boolean), status(int), deviceName, statusText, lastTime
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> checkDeviceStatus() {
        Map<String, Object> result = new HashMap<>();
        result.put("online", false);
        result.put("status", -1);
        result.put("deviceName", deviceName);
        result.put("statusText", "未知");

        if (accessKey == null || accessKey.isEmpty() ||
            productId == null || productId.isEmpty() ||
            deviceName == null || deviceName.isEmpty()) {
            log.warn("⏭️ OneNET未配置完整，无法查询设备状态");
            result.put("statusText", "未配置");
            return result;
        }

        String url = String.format(
            "%s/device/detail?product_id=%s&device_name=%s",
            baseUrl, productId, deviceName
        );

        try {
            log.info("🔄 正在查询OneNET设备状态...");

            String token = generateToken();
            if (token == null) {
                result.put("statusText", "Token生成失败");
                return result;
            }

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("authorization", token);
            headers.set("Content-Type", "application/json");

            org.springframework.http.HttpEntity<String> entity =
                new org.springframework.http.HttpEntity<>(headers);

            org.springframework.http.ResponseEntity<Map> response =
                restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, Map.class);

            Map<String, Object> body = response.getBody();
            log.info("   设备详情响应: {}", body != null ? body.toString() : "null");

            if (body == null) {
                log.warn("   响应体为空");
                result.put("statusText", "响应为空");
                return result;
            }

            Object codeObj = body.get("code");
            if (codeObj == null || !codeObj.toString().equals("0")) {
                log.warn("   OneNET业务错误: code={}, msg={}", codeObj, body.get("msg"));
                result.put("statusText", "API错误");
                return result;
            }

            Object dataObj = body.get("data");
            if (dataObj instanceof Map) {
                Map<String, Object> deviceData = (Map<String, Object>) dataObj;
                Object statusObj = deviceData.get("status");
                if (statusObj != null) {
                    int status = Integer.parseInt(statusObj.toString());
                    result.put("status", status);
                    result.put("online", status == 1);

                    switch (status) {
                        case 0:
                            result.put("statusText", "离线");
                            break;
                        case 1:
                            result.put("statusText", "在线");
                            break;
                        case 2:
                            result.put("statusText", "未激活");
                            break;
                        default:
                            result.put("statusText", "未知(" + status + ")");
                            break;
                    }
                }

                // 顺便获取设备最后在线时间
                if (deviceData.containsKey("last_time")) {
                    result.put("lastTime", deviceData.get("last_time"));
                }
            }

            log.info("✅ OneNET设备状态: {}", result.get("statusText"));
            return result;

        } catch (Exception e) {
            log.warn("  OneNET设备状态查询异常: {}", e.getMessage());
            result.put("statusText", "查询失败");
            return result;
        }
    }

    // ==================== OneNET token 生成 ====================

    private String generateToken() {
        try {
            if (accessKey == null || accessKey.isEmpty()) return null;

            String method = "sha1";
            String res = "products/" + productId;
            long et = System.currentTimeMillis() / 1000 + 3600;

            // 新版签名串格式: et + \n + method + \n + res + \n + version
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

            // URL编码 sign 和 res
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
