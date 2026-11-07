package com.fm.module.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 安全报告分页查询。
 */
@Schema(description = "安全报告分页查询")
@Data
public class SafetyReportQueryDTO {

    @Schema(description = "页码，从 1 开始")
    private long pageNum = 1;
    @Schema(description = "每页条数")
    private long pageSize = 10;
    @Schema(description = "类型")
    private Integer type;
    @Schema(description = "模糊搜索关键字")
    private String keyword;
}
