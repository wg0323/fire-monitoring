package com.fm.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * 全局共享的 RestTemplate：替代各业务类每次 restTemplateBuilder.build()，复用连接与线程资源。
 * 项目未引入 httpclient，故使用 JDK 版 SimpleClientHttpRequestFactory，不新增依赖。
 * 超时统一取原有各调用方上限（connect 5s / read 10s），业务侧不再单独设置超时。
 */
@Configuration
public class RestClientConfig {

    private static final int CONNECT_TIMEOUT_MILLIS = 5000;
    private static final int READ_TIMEOUT_MILLIS = 10000;

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        factory.setReadTimeout(READ_TIMEOUT_MILLIS);
        return new RestTemplate(factory);
    }
}
