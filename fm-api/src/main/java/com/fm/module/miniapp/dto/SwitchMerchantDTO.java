package com.fm.module.miniapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 切换门店请求。
 */
@Schema(description = "切换门店请求")
@Data
public class SwitchMerchantDTO {

    @Schema(description = "目标商户 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请选择商户")
    private Long merchantId;
}
