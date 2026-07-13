package com.classroom.onenet.pulsar.auth;

import org.apache.pulsar.client.api.Authentication;
import org.apache.pulsar.client.api.AuthenticationDataProvider;
import org.apache.pulsar.client.api.EncodedAuthenticationParameterSupport;
import org.apache.pulsar.client.api.PulsarClientException;

import java.io.IOException;
import java.util.Map;

/**
 * OneNET Pulsar 自定义鉴权实现
 * 鉴权方法名为 "iot-auth"
 * <p>
 * 对应原 SDK 中的 com.chinamobile.iot.pulsar.auth.IoTAuthentication
 */
public class IoTAuthentication implements Authentication, EncodedAuthenticationParameterSupport {

    private static final String methodName = "iot-auth";
    private String iotAccessId;
    private String iotSecretKey;

    public IoTAuthentication() {
    }

    public IoTAuthentication(String iotAccessId, String iotSecretKey) {
        this.iotAccessId = iotAccessId;
        this.iotSecretKey = iotSecretKey;
    }

    @Override
    public String getAuthMethodName() {
        return methodName;
    }

    @Override
    public AuthenticationDataProvider getAuthData() throws PulsarClientException {
        return new IoTAuthenticationDataProvider(this.iotAccessId, this.iotSecretKey);
    }

    @Override
    public void configure(String encodedAuthParamString) {
    }

    @Deprecated
    @Override
    public void configure(Map<String, String> authParams) {
    }

    @Override
    public void start() throws PulsarClientException {
    }

    @Override
    public void close() throws IOException {
    }
}
