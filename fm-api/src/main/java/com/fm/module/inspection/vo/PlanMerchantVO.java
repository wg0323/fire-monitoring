package com.fm.module.inspection.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 巡查计划商户项：附带本计划下的最新巡查状态。
 */
@Data
@Schema(description = "巡查计划商户项：附带本计划下的最新巡查状态")
public class PlanMerchantVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "名称")
    private String name;
    @Schema(description = "地址")
    private String addr;
    @Schema(description = "字段 finished")
    private Boolean finished;
    @Schema(description = "最后结果")
    private Integer lastResult;
    @Schema(description = "最后巡查by")
    private String lastPatrolBy;
    @Schema(description = "最后巡查时间")
    private LocalDateTime lastPatrolTime;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "隐患主键ID")
    private Long dangerId;
}
