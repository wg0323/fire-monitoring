package com.fm.module.alarm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 通话结果登记请求：电话告警与电话核警共用一个入口，均写入报警追踪表。
 */
@Schema(description = "通话结果登记请求")
@Data
public class AlarmCallDTO {

    /** 由 Controller 的路径参数写入，不参与请求体校验 */
    @Schema(description = "报警 ID，由路径参数写入")
    private Long id;

    @Schema(description = "被叫号码")
    @NotBlank(message = "被叫号码不能为空")
    private String calledNo;

    @Schema(description = "通话状态：dealing 已接通，notDeal 未接听，failed 呼叫失败")
    private String callState;

    @Schema(description = "通话类型：1 电话告警，2 电话核警；为空按电话核警处理")
    private Integer callType;

    @Schema(description = "联系人身份，如业主 / 第一责任人")
    private String contactName;

    @Schema(description = "是否接通；为空按已接通处理")
    private Boolean connected;

    @Schema(description = "外呼动作 ID；传入时更新同一条通话记录，避免重复登记")
    private String actionId;

    @Schema(description = "备注")
    private String remark;
}
