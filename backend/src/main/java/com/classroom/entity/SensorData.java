package com.classroom.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sensor_data")
public class SensorData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Float temperature;

    @Column(nullable = false)
    private Integer light;

    @Column(name = "led_status", nullable = false)
    private Integer ledStatus;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    public SensorData() {
    }

    public SensorData(Float temperature, Integer light, Integer ledStatus) {
        this.temperature = temperature;
        this.light = light;
        this.ledStatus = ledStatus;
    }

    @PrePersist
    public void prePersist() {
        if (this.createTime == null) {
            this.createTime = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Float getTemperature() {
        return temperature;
    }

    public void setTemperature(Float temperature) {
        this.temperature = temperature;
    }

    public Integer getLight() {
        return light;
    }

    public void setLight(Integer light) {
        this.light = light;
    }

    public Integer getLedStatus() {
        return ledStatus;
    }

    public void setLedStatus(Integer ledStatus) {
        this.ledStatus = ledStatus;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
