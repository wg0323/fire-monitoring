package com.fm.module.danger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 隐患上报请求：随手拍与巡查共用，来源由调用方指定。
 */
@Schema(description = "隐患上报请求")
@Data
public class DangerSaveDTO {

    @NotNull
    @Schema(description = "企业 ID")
    private Long merchantId;
    @Schema(description = "隐患类型")
    private String dangerType;
    @NotBlank
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "经度")
    private BigDecimal lng;
    @Schema(description = "纬度")
    private BigDecimal lat;
    @Schema(description = "具体位置")
    private String position;
    @Schema(description = "位置描述")
    private String positionDetail;
    @Schema(description = "现场照片 1")
    private String pic1;
    @Schema(description = "现场照片 2")
    private String pic2;
    @Schema(description = "现场照片 3")
    private String pic3;
    @Schema(description = "隐患等级")
    private Integer dangerLevel;
    @Schema(description = "整改期限")
    private LocalDate dealLimit;
}
