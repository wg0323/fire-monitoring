package com.fm.module.workorder.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 工单分页查询。
 */
@Schema(description = "工单分页查询")
@Data
public class WorkOrderQueryDTO {

    @Schema(description = "页码，从 1 开始")
    private long pageNum = 1;
    @Schema(description = "每页条数")
    private long pageSize = 10;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "企业 ID")
    private Long merchantId;
    @Schema(description = "模糊搜索关键字")
    private String keyword;
}
