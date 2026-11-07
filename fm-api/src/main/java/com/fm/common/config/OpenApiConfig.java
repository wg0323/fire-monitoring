package com.fm.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 文档元信息。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI fmOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("消防监控平台 API")
                        .version("1.0.0")
                        .description("后台管理 /api/admin 与小程序 /api/mp 接口文档"));
    }
}
