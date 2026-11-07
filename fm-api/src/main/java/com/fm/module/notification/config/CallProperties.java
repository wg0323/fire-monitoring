package com.fm.module.notification.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 外呼配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "fm.call")
public class CallProperties {

    /**
     * true=不调用第三方外呼接口，直接返回模拟成功，便于无话机联调。
     */
    private boolean mock;
}
