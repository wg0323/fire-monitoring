package com.fm.module.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 字典项保存请求。设备类型额外使用离线阈值与监控标记。
 */
@Schema(description = "字典项保存请求")
@Data
public class DictItemDTO {

    @Schema(description = "主键 ID")
    private Long id;
    @Schema(description = "编码")
    private String code;
    @Schema(description = "名称")
    private String name;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "排序号")
    private Integer sort;
    @Schema(description = "离线阈值")
    private Integer offlineThreshold;
    @Schema(description = "离线时长单位")
    private String offlineUnit;
    @Schema(description = "是否监测")
    private Integer monitorFlag;
}
