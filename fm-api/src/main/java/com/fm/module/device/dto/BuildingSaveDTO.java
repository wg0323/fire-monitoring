package com.fm.module.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 楼宇新增/修改请求。
 */
@Schema(description = "楼宇新增/修改请求")
@Data
public class BuildingSaveDTO {

    @Schema(description = "主键 ID")
    private Long id;
    @Schema(description = "企业 ID")
    private Long merchantId;
    @Schema(description = "名称")
    private String name;
    @Schema(description = "排序号")
    private Integer sort;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "状态")
    private Integer status;
}
