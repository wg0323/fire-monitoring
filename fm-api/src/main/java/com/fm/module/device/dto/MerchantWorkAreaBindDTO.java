package com.fm.module.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 商户绑定工作区请求。
 */
@Schema(description = "商户绑定工作区请求")
@Data
public class MerchantWorkAreaBindDTO {

    @Schema(description = "工作区 ID 列表")
    private List<Long> workAreaIds;
}
