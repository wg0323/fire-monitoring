package com.fm.module.danger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

/**
 * 隐患整改提交：责任人填写说明与照片。
 */
@Schema(description = "隐患整改提交")
@Data
public class DangerDealDTO {

    @NotBlank
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
