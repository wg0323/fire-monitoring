package com.fm.module.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 商户分页查询条件。
 */
@Schema(description = "商户分页查询条件")
@Data
public class MerchantQueryDTO {

    @Schema(description = "页码，从 1 开始")
    private long pageNum = 1;
    @Schema(description = "每页条数")
    private long pageSize = 10;
    @Schema(description = "模糊搜索关键字")
    private String keyword;
    @Schema(description = "类型")
    private String type;
    @Schema(description = "企业性质")
    private Integer merchantKind;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "工作区 ID")
    private Long workAreaId;
}
