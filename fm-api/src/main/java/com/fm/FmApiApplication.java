package com.fm;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 业务单体启动入口。
 */
@SpringBootApplication
@EnableScheduling
@MapperScan({"com.fm.module.system.mapper", "com.fm.module.device.mapper",
        "com.fm.module.alarm.mapper", "com.fm.module.notification.mapper",
        "com.fm.module.danger.mapper", "com.fm.module.inspection.mapper",
        "com.fm.module.workorder.mapper", "com.fm.module.report.mapper",
        "com.fm.module.monitor.mapper"})
public class FmApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FmApiApplication.class, args);
    }
}
