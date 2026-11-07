package com.fm.module.miniapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 小程序手机号登录请求：正式环境传 code，开发环境可传 phone。
 */
@Schema(description = "小程序手机号登录请求")
@Data
public class PhoneLoginDTO {

    @Schema(description = "编码")
    private String code;
    @Schema(description = "手机号")
    private String phone;
}
