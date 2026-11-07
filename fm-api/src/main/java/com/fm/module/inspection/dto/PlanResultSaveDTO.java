package com.fm.module.inspection.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 巡查结果提交：发现隐患时同时生成并指派商户整改。
 */
@Schema(description = "巡查结果提交")
@Data
public class PlanResultSaveDTO {

    @NotNull
    @Schema(description = "计划 ID")
    private Long planId;
    @NotNull
    @Schema(description = "企业 ID")
    private Long merchantId;
    @NotNull
    @Schema(description = "处理结果")
    private Integer result;
    @Schema(description = "隐患类型")
    private String dangerType;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "现场照片 1")
    private String pic1;
    @Schema(description = "现场照片 2")
    private String pic2;
    @Schema(description = "现场照片 3")
    private String pic3;
    @Schema(description = "经度")
    private BigDecimal lng;
    @Schema(description = "纬度")
    private BigDecimal lat;
}
