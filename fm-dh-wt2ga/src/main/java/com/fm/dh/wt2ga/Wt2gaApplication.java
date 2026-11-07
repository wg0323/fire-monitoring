package com.fm.dh.wt2ga;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 感温探测器采集器启动入口：只接收 CTWing 推送并投递 MQ，不写业务库。
 */
@EnableScheduling
@SpringBootApplication
public class Wt2gaApplication {

    public static void main(String[] args) {
        SpringApplication.run(Wt2gaApplication.class, args);
    }
}
