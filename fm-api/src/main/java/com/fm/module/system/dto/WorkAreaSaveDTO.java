package com.fm.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 工作区新增/修改请求，成员与商户按全量替换处理。
 */
@Schema(description = "工作区新增/修改请求，成员与商户按全量替换处理")
@Data
public class WorkAreaSaveDTO {

    @Schema(description = "主键 ID")
    private Long id;
    @Schema(description = "租户 ID")
    private Long tenantId;
    @Schema(description = "名称")
    private String name;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "用户 ID 列表")
    private List<Long> userIds;
    @Schema(description = "企业 ID 列表")
    private List<Long> merchantIds;
}
