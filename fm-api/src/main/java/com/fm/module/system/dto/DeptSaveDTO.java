package com.fm.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 部门新增/修改请求。
 */
@Schema(description = "部门新增/修改请求")
@Data
public class DeptSaveDTO {

    @Schema(description = "主键 ID")
    private Long id;
    @Schema(description = "租户 ID")
    private Long tenantId;
    @Schema(description = "上级 ID")
    private Long parentId;
    @Schema(description = "名称")
    private String name;
    @Schema(description = "状态")
    private Integer status;
}
