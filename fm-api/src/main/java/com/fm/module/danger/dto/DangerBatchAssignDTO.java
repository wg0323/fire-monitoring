package com.fm.module.danger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * 隐患批量指派请求：一次把多条待指派隐患指派给同一名外勤。
 */
@Schema(description = "隐患批量指派请求")
@Data
public class DangerBatchAssignDTO {

    /** 单次提交上限，与后台批量操作的通行做法一致，避免一次请求产生过大事务 */
    public static final int MAX_SIZE = 100;

    @NotEmpty(message = "请选择要指派的隐患")
    @Size(max = MAX_SIZE, message = "单次最多指派 100 条隐患")
    @Schema(description = "隐患 ID 列表，单次最多 100 条", required = true)
    private List<Long> ids;

    @NotNull(message = "请选择外勤")
    @Schema(description = "处理人用户 ID", required = true)
    private Long assigneeId;

    @Schema(description = "整改期限")
    private LocalDate dealLimit;

    @Schema(description = "隐患最终等级")
    private Integer dangerLevelFinal;

    @Schema(description = "备注")
    private String remark;
}
