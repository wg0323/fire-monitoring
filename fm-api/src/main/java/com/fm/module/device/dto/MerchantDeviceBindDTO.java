package com.fm.module.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 商户绑定设备请求。
 */
@Schema(description = "商户绑定设备请求")
@Data
public class MerchantDeviceBindDTO {

    @Schema(description = "设备 ID 列表")
    private List<Long> deviceIds;
}
