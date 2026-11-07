package com.fm.module.danger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 隐患分页查询条件。
 */
@Schema(description = "隐患分页查询条件")
@Data
public class DangerQueryDTO {

    @Schema(description = "页码，从 1 开始")
    private long pageNum = 1;
    @Schema(description = "每页条数")
    private long pageSize = 10;
    @Schema(description = "隐患状态")
    private Integer status;
    @Schema(description = "是否只看未完结（待指派/待整改/待审核）")
    private Boolean unfinished;
    @Schema(description = "来源类型")
    private Integer sourceType;
    @Schema(description = "企业 ID")
    private Long merchantId;
    @Schema(description = "隐患类型")
    private String dangerType;
    @Schema(description = "关键字（编号/位置）")
    private String keyword;
    @Schema(description = "整改期限起")
    private LocalDate dealLimitFrom;
    @Schema(description = "整改期限止")
    private LocalDate dealLimitTo;
}
