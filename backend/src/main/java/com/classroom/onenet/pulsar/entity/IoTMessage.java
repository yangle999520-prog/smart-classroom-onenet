package com.classroom.onenet.pulsar.entity;

import java.io.Serializable;

/**
 * OneNET Pulsar 消息体
 * 对应原 SDK 中的 com.chinamobile.iot.pulsar.auth.IoTMessage
 */
public class IoTMessage implements Serializable {

    /** AES 加密后的设备数据（Base64 编码） */
    private String data;

    /** 超级消息标志 */
    private Integer superMsg;

    /** 协议版本，默认 "1.0" */
    private String pv = "1.0";

    /** 时间戳（毫秒） */
    private Long t = System.currentTimeMillis();

    /** 签名 */
    private String sign;

    // ==================== Getters & Setters ====================

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Integer getSuperMsg() {
        return superMsg;
    }

    public void setSuperMsg(Integer superMsg) {
        this.superMsg = superMsg;
    }

    public String getPv() {
        return pv;
    }

    public void setPv(String pv) {
        this.pv = pv;
    }

    public Long getT() {
        return t;
    }

    public void setT(Long t) {
        this.t = t;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    @Override
    public String toString() {
        return "IoTMessage{" +
                "superMsg=" + superMsg +
                ", pv='" + pv + '\'' +
                ", t=" + t +
                ", sign='" + sign + '\'' +
                ", data.length=" + (data != null ? data.length() : 0) +
                '}';
    }
}
