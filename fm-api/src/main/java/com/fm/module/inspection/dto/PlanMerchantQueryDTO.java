package com.fm.module.inspection.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 计划工作区商户分页查询。
 */
@Schema(description = "计划工作区商户分页查询")
@Data
public class PlanMerchantQueryDTO {

    @Schema(description = "页码，从 1 开始")
    private long pageNum = 1;
    @Schema(description = "每页条数")
    private long pageSize = 10;
    @Schema(description = "商户名称，模糊匹配")
    private String name;
    @Schema(description = "巡查状态：0 未巡查，1 正常，2 发现隐患；不传表示全部")
    private Integer patrolStatus;
}
