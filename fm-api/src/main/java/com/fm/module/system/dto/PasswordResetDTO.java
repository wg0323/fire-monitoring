package com.fm.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 重置密码请求：密码走请求体，避免出现在访问日志。
 */
@Schema(description = "重置密码请求")
@Data
public class PasswordResetDTO {

    @Schema(description = "密码")
    private String password;
}
