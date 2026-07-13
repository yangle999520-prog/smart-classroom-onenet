package com.classroom.service;

import com.classroom.entity.SensorData;
import com.classroom.repository.SensorDataRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SensorDataService {

    private final SensorDataRepository repository;

    public SensorDataService(SensorDataRepository repository) {
        this.repository = repository;
    }

    /** 保存传感器数据 */
    public SensorData saveData(SensorData data) {
        return repository.save(data);
    }

    /** 获取最新一条传感器数据 */
    public Optional<SensorData> getLatestData() {
        return repository.findTopByOrderByCreateTimeDesc();
    }

    /** 获取最近N小时的历史数据 */
    public List<SensorData> getHistoryByHours(int hours) {
        if (hours <= 0) hours = 24;
        if (hours > 720) hours = 720; // 最多30天
        return repository.findLastNHours(hours);
    }

    /** 获取最近N天的历史数据 */
    public List<SensorData> getHistoryByDays(int days) {
        if (days <= 0) days = 7;
        if (days > 90) days = 90;
        return repository.findLastNDays(days);
    }

    /** 分页查询所有数据 */
    public List<SensorData> getAllData(int page, int size) {
        return repository.findAll(
                org.springframework.data.domain.PageRequest.of(page, size,
                        org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createTime"))
        ).getContent();
    }

    /** 获取统计数据 */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        List<SensorData> recentData = repository.findLastNHours(24);

        if (recentData.isEmpty()) {
            stats.put("totalRecords", repository.count());
            return stats;
        }

        double avgTemp = recentData.stream().mapToDouble(SensorData::getTemperature).average().orElse(0);
        double maxTemp = recentData.stream().mapToDouble(SensorData::getTemperature).max().orElse(0);
        double minTemp = recentData.stream().mapToDouble(SensorData::getTemperature).min().orElse(0);
        double avgLight = recentData.stream().mapToInt(SensorData::getLight).average().orElse(0);

        long ledOnCount = recentData.stream().filter(d -> d.getLedStatus() == 1).count();
        long manualModeCount = recentData.stream().filter(d -> d.getMode() == 1).count();

        stats.put("totalRecords", repository.count());
        stats.put("avgTemperature24h", String.format("%.1f", avgTemp));
        stats.put("maxTemperature24h", String.format("%.1f", maxTemp));
        stats.put("minTemperature24h", String.format("%.1f", minTemp));
        stats.put("avgLight24h", String.format("%.0f", avgLight));
        stats.put("ledOnRate24h", String.format("%.1f", recentData.isEmpty() ? 0 : (ledOnCount * 100.0 / recentData.size())));
        stats.put("currentMode", recentData.get(0).getMode());  // 最新一条的模式

        return stats;
    }
}
