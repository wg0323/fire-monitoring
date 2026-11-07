package com.fm.module.inspection.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 巡查计划分页查询。
 */
@Schema(description = "巡查计划分页查询")
@Data
public class PlanQueryDTO {

    @Schema(description = "页码，从 1 开始")
    private long pageNum = 1;
    @Schema(description = "每页条数")
    private long pageSize = 10;
    @Schema(description = "名称")
    private String name;
    @Schema(description = "工作区 ID")
    private Long workAreaId;
}
