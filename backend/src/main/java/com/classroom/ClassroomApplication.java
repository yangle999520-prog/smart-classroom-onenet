package com.classroom;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // ✅ 启用定时任务，OneNET数据拉取才能运行
public class ClassroomApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClassroomApplication.class, args);
    }
}
