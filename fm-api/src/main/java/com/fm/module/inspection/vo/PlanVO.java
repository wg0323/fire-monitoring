package com.fm.module.inspection.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 巡查计划展示对象，附带工作区、推导状态与完成进度。
 */
@Data
@Schema(description = "巡查计划展示对象，附带工作区、推导状态与完成进度")
public class PlanVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键ID")
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "租户主键ID")
    private Long tenantId;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "工作区域主键ID")
    private Long workAreaId;
    @Schema(description = "工作区域名称")
    private String workAreaName;
    @Schema(description = "名称")
    private String name;
    @Schema(description = "开始日期")
    private LocalDate startDate;
    @Schema(description = "结束日期")
    private LocalDate endDate;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "商户总数")
    private Long merchantTotal;
    @Schema(description = "finish数量")
    private Long finishCount;
    @Schema(description = "隐患数量")
    private Long dangerCount;
    @Schema(description = "创建by")
    private String createBy;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
