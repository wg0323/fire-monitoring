package com.fm.module.danger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 隐患审核请求：1 通过，4 驳回；驳回必须填写意见。
 */
@Schema(description = "隐患审核请求")
@Data
public class DangerAuditDTO {

    public static final int RESULT_PASS = 1;
    public static final int RESULT_REJECT = 4;

    @NotNull
    @Schema(description = "审核结果")
    private Integer auditResult;
    @Schema(description = "审核意见")
    private String auditRemark;
    @Schema(description = "审核照片 1")
    private String auditPic1;
    @Schema(description = "审核照片 2")
    private String auditPic2;
    @Schema(description = "审核照片 3")
    private String auditPic3;
}
