package com.fm.module.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 安全报告保存请求。
 */
@Schema(description = "安全报告保存请求")
@Data
public class SafetyReportSaveDTO {

    @NotNull
    @Schema(description = "类型")
    private Integer type;
    @NotBlank
    @Schema(description = "报告名称")
    private String reportName;
    @Schema(description = "文件地址")
    private String filePath;
    @Schema(description = "报告时间")
    private LocalDate reportTime;
}
