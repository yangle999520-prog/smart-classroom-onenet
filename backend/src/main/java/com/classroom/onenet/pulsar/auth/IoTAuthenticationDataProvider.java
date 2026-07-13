package com.classroom.onenet.pulsar.auth;

import org.apache.pulsar.client.api.AuthenticationDataProvider;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * OneNET Pulsar 鉴权数据提供者
 * <p>
 * 对应原 SDK 中的 com.chinamobile.iot.pulsar.auth.IoTAuthenticationDataProvider
 * 已将 pulsar-shade 的 DigestUtils 替换为标准 java.security.MessageDigest
 * <p>
 * Token 生成逻辑:
 * password = SHA256(iotAccessId + SHA256(iotSecretKey)).substring(4, 20)
 */
public class IoTAuthenticationDataProvider implements AuthenticationDataProvider {

    private String token;
    private static final String methodName = "iot-auth";

    public IoTAuthenticationDataProvider() {
    }

    public IoTAuthenticationDataProvider(String iotAccessId, String iotSecretKey) {
        this.token = String.format(
                "{\"tenant\":\"%s\",\"password\":\"%s\"}",
                iotAccessId,
                sha256Hex(iotAccessId + sha256Hex(iotSecretKey)).substring(4, 20)
        );
    }

    @Override
    public boolean hasDataForHttp() {
        return false;
    }

    @Override
    public Set<Map.Entry<String, String>> getHttpHeaders() throws Exception {
        return null;
    }

    @Override
    public boolean hasDataFromCommand() {
        return true;
    }

    @Override
    public String getCommandData() {
        return token;
    }

    /**
     * 计算 SHA-256 十六进制字符串
     */
    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
