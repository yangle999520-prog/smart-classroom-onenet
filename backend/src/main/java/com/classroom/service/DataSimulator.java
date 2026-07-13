package com.classroom.service;

import com.classroom.entity.SensorData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Random;

/**
 * 数据模拟器
 * 在开发/测试环境中自动生成模拟传感器数据（光照值为百分比 0~100）
 * 通过配置 simulator.enabled=true 启用
 */
@Component
@ConditionalOnProperty(name = "simulator.enabled", havingValue = "true")
public class DataSimulator implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSimulator.class);
    private final SensorDataService sensorDataService;
    private final Random random = new Random();

    public DataSimulator(SensorDataService sensorDataService) {
        this.sensorDataService = sensorDataService;
    }

    @Override
    public void run(String... args) {
        log.info("==================== 数据模拟器已启动 ====================");
        log.info("正在生成模拟传感器数据（光照值: 0~100%）...");

        int count = 0;

        // 生成过去24小时的模拟数据（每10分钟一条）
        LocalDateTime baseTime = LocalDateTime.now().minusHours(24);
        for (int i = 0; i < 144; i++) {
            LocalDateTime time = baseTime.plusMinutes(i * 10);

            // 模拟一天内的温度变化（22~32度）
            double hourFactor = Math.sin(Math.PI * time.getHour() / 24);
            float temperature = 24.0f + (float) (hourFactor * 5.0f) + (float) (random.nextGaussian() * 0.5);

            // 模拟光照变化（百分比 0~100，白天高，夜晚低）
            int hour = time.getHour();
            int light;
            int ledStatus;
            int mode = 0;   // 默认自动模式
            if (hour >= 8 && hour <= 18) {
                // 白天：60~100%
                light = 60 + random.nextInt(41);
                ledStatus = 0;
            } else if (hour >= 6 && hour < 8) {
                // 清晨：15~45%
                light = 15 + random.nextInt(31);
                ledStatus = light < 30 ? 1 : 0;
            } else if (hour > 18 && hour <= 20) {
                // 傍晚：10~35%
                light = 10 + random.nextInt(26);
                ledStatus = 1;
            } else {
                // 夜晚：0~15%
                light = random.nextInt(16);
                ledStatus = 1;
            }

            // 偶尔加入异常数据测试滤波
            if (i % 50 == 0) {
                temperature += 5.0f;
            }

            SensorData data = new SensorData();
            data.setTemperature((float) Math.round(temperature * 10) / 10.0f);
            data.setLight(light);
            data.setLedStatus(ledStatus);
            data.setMode(mode);
            data.setCreateTime(time);

            sensorDataService.saveData(data);
            count++;
        }

        log.info("数据模拟完成！共生成 {} 条传感器数据", count);
        log.info("======================================================");
    }
}
