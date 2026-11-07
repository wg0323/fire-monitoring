package com.fm.module.report.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 安全报告展示对象。
 */
@Data
@Schema(description = "安全报告展示对象")
public class SafetyReportVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键ID")
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "租户主键ID")
    private Long tenantId;
    @Schema(description = "类型")
    private Integer type;
    @Schema(description = "report名称")
    private String reportName;
    @Schema(description = "文件路径")
    private String filePath;
    @Schema(description = "report时间")
    private LocalDate reportTime;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
