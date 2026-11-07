package com.fm.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 运营中心分页查询条件。
 */
@Schema(description = "运营中心分页查询条件")
@Data
public class OrgQueryDTO {

    @Schema(description = "页码，从 1 开始")
    private long pageNum = 1;
    @Schema(description = "每页条数")
    private long pageSize = 20;
    @Schema(description = "模糊搜索关键字")
    private String keyword;
    @Schema(description = "状态")
    private Integer status;
}
