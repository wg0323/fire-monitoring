package com.fm.module.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 外呼配置保存请求。
 */
@Schema(description = "外呼配置保存请求")
@Data
public class CallConfigDTO {

    /** 仅允许 https + 七陌官方域名（含子域），禁止路径、userinfo、自定义 scheme。 */
    public static final String HOST_PATTERN = "(?i)^https://([a-z0-9-]+\\.)*7moor\\.com(:[0-9]{1,5})?/?$";

    @NotBlank
    @Schema(description = "账号 ID")
    private String accountId;

    @NotBlank
    @Schema(description = "接口密钥")
    private String apiSecret;

    @NotBlank
    @Pattern(regexp = HOST_PATTERN, message = "接口地址仅支持 https 的七陌官方域名")
    @Schema(description = "接口地址，仅支持 https 的七陌官方域名，如 https://api.7moor.com")
    private String host;

    @Schema(description = "外呼分机号")
    private String fromExten;

    @Schema(description = "状态")
    private Integer status;
}
