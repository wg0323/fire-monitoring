package com.fm.module.alarm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 派单请求：先选工作区，再从工作区成员中选外勤。
 */
@Schema(description = "报警派单请求")
@Data
public class AlarmDispatchDTO {

    /** 由后台请求体或小程序路径参数写入 */
    @Schema(description = "报警 ID，由路径参数写入")
    private Long id;

    @Schema(description = "工作区 ID")
    @NotNull(message = "请选择工作区")
    private Long workAreaId;

    @Schema(description = "处理人用户 ID")
    @NotNull(message = "请选择外勤")
    private Long handlerId;

    @Schema(description = "派单说明")
    private String dispatchRemark;
}
