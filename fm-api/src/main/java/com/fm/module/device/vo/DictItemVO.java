package com.fm.module.device.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 字典管理列表项。
 */
@Data
@Schema(description = "字典管理列表项")
public class DictItemVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "编码")
    private String code;
    @Schema(description = "名称")
    private String name;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "排序")
    private Integer sort;
    @Schema(description = "离线threshold")
    private Integer offlineThreshold;
    @Schema(description = "离线单位")
    private String offlineUnit;
    @Schema(description = "监控标志")
    private Integer monitorFlag;
}
