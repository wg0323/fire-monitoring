package com.fm.module.danger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 隐患指派请求：故障转隐患由管理员指派给所属工作区外勤。
 */
@Schema(description = "隐患指派请求")
@Data
public class DangerAssignDTO {

    @NotNull
    @Schema(description = "处理人用户 ID")
    private Long assigneeId;
    @Schema(description = "整改期限")
    private LocalDate dealLimit;
    @Schema(description = "隐患最终等级")
    private Integer dangerLevelFinal;
    @Schema(description = "备注")
    private String remark;
}
