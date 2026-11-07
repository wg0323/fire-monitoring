package com.fm.module.inspection.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 巡查计划新增/修改请求：按工作区创建，工作区必填。
 */
@Schema(description = "巡查计划新增/修改请求")
@Data
public class PlanSaveDTO {

    @Schema(description = "主键 ID")
    private Long id;
    @NotNull
    @Schema(description = "工作区 ID")
    private Long workAreaId;
    @NotBlank
    @Schema(description = "名称")
    private String name;
    @NotNull
    @Schema(description = "开始日期")
    private LocalDate startDate;
    @NotNull
    @Schema(description = "结束日期")
    private LocalDate endDate;
    @Schema(description = "备注")
    private String remark;
}
