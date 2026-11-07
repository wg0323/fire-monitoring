package com.fm.module.device.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 商户详情：基本信息 + 楼宇楼层 + 设备 + 工作区。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "商户详情：基本信息 + 楼宇楼层 + 设备 + 工作区")
public class MerchantDetailVO extends MerchantVO {

    @Schema(description = "工作区域")
    private List<WorkAreaBriefVO> workAreas;
    @Schema(description = "楼宇")
    private List<BuildingNodeVO> buildings;
    @Schema(description = "设备")
    private List<MerchantDeviceBriefVO> devices;
}
