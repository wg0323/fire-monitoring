package com.fm.module.miniapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 微信小程序登录请求：code 由 wx.login 获取。
 */
@Schema(description = "微信小程序登录请求")
@Data
public class WxLoginDTO {

    @NotBlank
    @Schema(description = "编码")
    private String code;
}
