package com.classroom.controller;

import com.classroom.entity.SensorData;
import com.classroom.service.SensorDataService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/sensor")
public class SensorDataController {

    private final SensorDataService sensorDataService;

    public SensorDataController(SensorDataService sensorDataService) {
        this.sensorDataService = sensorDataService;
    }

    // ==================== 数据上传接口 ====================

    /**
     * 上传传感器数据（模拟ESP8266/OneNET推送）
     * POST /api/sensor/upload
     * Body: { "temperature": 25.6, "light": 500, "ledStatus": 0 }
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadData(@RequestBody Map<String, Object> body) {
        Map<String, Object> response = new HashMap<>();

        try {
            Float temperature = body.containsKey("temperature") ?
                    Float.parseFloat(body.get("temperature").toString()) : 0f;
            Integer light = body.containsKey("light") ?
                    Integer.parseInt(body.get("light").toString()) : 0;
            Integer ledStatus = body.containsKey("ledStatus") || body.containsKey("led") ?
                    Integer.parseInt(body.getOrDefault("ledStatus", body.get("led")).toString()) : 0;

            SensorData data = new SensorData(temperature, light, ledStatus);
            SensorData saved = sensorDataService.saveData(data);

            response.put("code", 200);
            response.put("message", "数据上传成功");
            response.put("data", saved);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "数据上传失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ==================== 数据查询接口 ====================

    /** 获取最新一条传感器数据 */
    @GetMapping("/latest")
    public ResponseEntity<Map<String, Object>> getLatestData() {
        Map<String, Object> response = new HashMap<>();
        Optional<SensorData> latest = sensorDataService.getLatestData();

        if (latest.isPresent()) {
            response.put("code", 200);
            response.put("data", latest.get());
        } else {
            response.put("code", 200);
            response.put("data", null);
            response.put("message", "暂无数据");
        }
        return ResponseEntity.ok(response);
    }

    /** 获取历史数据（默认最近24小时） */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getHistory(
            @RequestParam(value = "hours", defaultValue = "24") int hours) {
        Map<String, Object> response = new HashMap<>();
        List<SensorData> list = sensorDataService.getHistoryByHours(hours);

        response.put("code", 200);
        response.put("data", list);
        response.put("total", list.size());
        return ResponseEntity.ok(response);
    }

    /** 分页查询所有数据 */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getList(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        Map<String, Object> response = new HashMap<>();
        List<SensorData> list = sensorDataService.getAllData(page, size);

        response.put("code", 200);
        response.put("data", list);
        response.put("page", page);
        response.put("size", size);
        return ResponseEntity.ok(response);
    }

    /** 获取统计数据 */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> stats = sensorDataService.getStatistics();

        response.put("code", 200);
        response.put("data", stats);
        return ResponseEntity.ok(response);
    }
}
