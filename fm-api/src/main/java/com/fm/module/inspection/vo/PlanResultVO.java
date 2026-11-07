package com.fm.module.inspection.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 巡查结果展示对象。
 */
@Data
@Schema(description = "巡查结果展示对象")
public class PlanResultVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键ID")
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "计划主键ID")
    private Long planId;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "商户主键ID")
    private Long merchantId;
    @Schema(description = "商户名称")
    private String merchantName;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "用户主键ID")
    private Long userId;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "工作区域主键ID")
    private Long workAreaId;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "隐患主键ID")
    private Long dangerId;
    @Schema(description = "巡查by")
    private String patrolBy;
    @Schema(description = "巡查时间")
    private LocalDateTime patrolTime;
    @Schema(description = "经度")
    private BigDecimal lng;
    @Schema(description = "纬度")
    private BigDecimal lat;
    @Schema(description = "结果")
    private Integer result;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "字段 pic1")
    private String pic1;
    @Schema(description = "字段 pic2")
    private String pic2;
    @Schema(description = "字段 pic3")
    private String pic3;
}
