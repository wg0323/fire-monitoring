package com.fm.dh.wt2ga.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 设备注册表配置：刷新间隔与只加载的设备类型。
 */
@Data
@Component
@ConfigurationProperties(prefix = "fm.registry")
public class RegistryProperties {

    private long refreshIntervalMs;
    private String deviceType;
}
