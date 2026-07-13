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
 * 在开发/调试阶段生成模拟传感器数据，支持脱离硬件独立运行
 *
 * 启动时一次性生成过去24小时的模拟数据（每10分钟一条，共144条）
 * 模拟温度和光照的昼夜变化规律，更贴近真实场景
 */
@Component
@ConditionalOnProperty(name = "simulator.enabled", havingValue = "true")
public class DataSimulator implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSimulator.class);

    private final SensorDataService sensorDataService;

    public DataSimulator(SensorDataService sensorDataService) {
        this.sensorDataService = sensorDataService;
    }

    @Override
    public void run(String... args) {
        log.info("======================================================");
        log.info("🚀 数据模拟器已启动，开始生成模拟数据...");

        long existingCount = sensorDataService.getStatistics().getOrDefault("totalRecords", 0) instanceof Number
                ? ((Number) sensorDataService.getStatistics().getOrDefault("totalRecords", 0)).longValue()
                : 0;

        if (existingCount > 10) {
            log.info("⏭️ 数据库中已有 {} 条数据，跳过模拟数据生成", existingCount);
            return;
        }

        Random random = new Random(42); // 固定种子，每次生成一致的数据
        LocalDateTime now = LocalDateTime.now();
        int totalGenerated = 0;

        // 生成过去24小时的数据，每10分钟一条
        for (int i = 143; i >= 0; i--) {
            LocalDateTime time = now.minusMinutes(i * 10L);

            // 模拟昼夜温度变化：白天高、夜间低
            int hour = time.getHour();
            double baseTemp;
            if (hour >= 8 && hour <= 18) {          // 白天（8:00-18:00）
                baseTemp = 26.0 + 4.0 * Math.sin((hour - 8) * Math.PI / 10);
            } else if (hour >= 19 || hour <= 5) {    // 夜间（19:00-05:00）
                baseTemp = 22.0 - 2.0 * Math.sin((hour - 19) * Math.PI / 10);
            } else {                                   // 清晨（6:00-7:00）
                baseTemp = 20.0 + 3.0 * (hour - 6);
            }
            float temperature = (float) (baseTemp + random.nextGaussian() * 0.8);

            // 模拟光照变化：白天有光照、夜间无光照
            int light;
            if (hour >= 8 && hour <= 18) {
                // 白天：光照 40-100%，模拟教室窗帘/天气变化
                light = 40 + random.nextInt(61);
            } else if (hour >= 6 && hour <= 7) {
                // 清晨：光照 10-40%
                light = 10 + random.nextInt(31);
            } else {
                // 夜间：光照 0-15%
                light = random.nextInt(16);
            }

            // LED状态：光照<30% 且为自动模式时开灯
            int ledStatus = (light < 30) ? 1 : 0;

            // 工作模式：大部分时间为自动模式(0)
            int mode = random.nextInt(10) == 0 ? 1 : 0;

            SensorData data = new SensorData(temperature, light, ledStatus, mode);
            data.setCreateTime(time);
            sensorDataService.saveData(data);
            totalGenerated++;
        }

        log.info("✅ 数据模拟器完成，共生成了 {} 条模拟数据（24小时 / 每10分钟）", totalGenerated);
        log.info("======================================================");
    }
}
