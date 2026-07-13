package com.classroom.onenet.pulsar.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;

/**
 * AES 解密工具
 * 用于解密 OneNET Pulsar 消息中的设备数据
 * <p>
 * 对应原 SDK 中的 com.chinamobile.iot.pulsar.auth.AESBase64Utils
 * 已将 pulsar-shade 的 Base64 替换为标准 java.util.Base64
 */
public class AESBase64Utils {

    private static final String ALGO = "AES";
    private byte[] keyValue;

    /**
     * 解密 Base64 编码的 AES 加密数据
     *
     * @param encryptedData Base64 编码的加密数据
     * @return 解密后的明文字符串
     * @throws Exception 解密失败时抛出
     */
    public String decrypt(String encryptedData) throws Exception {
        Key key = generateKey();
        Cipher c = Cipher.getInstance(ALGO);
        c.init(Cipher.DECRYPT_MODE, key);
        byte[] decodedValue = Base64.getDecoder().decode(encryptedData);
        byte[] decValue = c.doFinal(decodedValue);
        return new String(decValue, StandardCharsets.UTF_8);
    }

    private Key generateKey() {
        return new SecretKeySpec(keyValue, ALGO);
    }

    public void setKeyValue(byte[] keyValue) {
        this.keyValue = keyValue;
    }

    /**
     * 便捷静态方法：使用指定密钥解密数据
     *
     * @param data      Base64 编码的加密数据
     * @param secretKey AES 密钥字符串（16字节）
     * @return 解密后的明文字符串
     * @throws Exception 解密失败时抛出
     */
    public static String decrypt(String data, String secretKey) throws Exception {
        AESBase64Utils aes = new AESBase64Utils();
        aes.setKeyValue(secretKey.getBytes(StandardCharsets.UTF_8));
        return aes.decrypt(data);
    }
}
