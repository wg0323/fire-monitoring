package com.fm.module.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 商户启用/禁用请求。
 */
@Schema(description = "商户启用/禁用请求")
@Data
public class MerchantStatusDTO {

    @Schema(description = "状态")
    private Integer status;
}
