package com.fm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 业务单体启动入口。
 */
@SpringBootApplication
@EnableScheduling
public class FmApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FmApiApplication.class, args);
    }
}
