package com.fm.module.monitor.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 电子档案树：商户节点。
 */
@Data
@Schema(description = "电子档案树：商户节点")
public class ArchiveMerchantVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "商户主键ID")
    private Long merchantId;
    @Schema(description = "商户名称")
    private String merchantName;
    @Schema(description = "楼宇")
    private List<ArchiveBuildingVO> buildings;
    @Schema(description = "设备")
    private List<MonitorDeviceVO> devices;
}
