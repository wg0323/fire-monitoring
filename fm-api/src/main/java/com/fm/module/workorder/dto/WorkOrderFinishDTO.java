package com.fm.module.workorder.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 工单完成请求：外勤填写结论、说明与照片，同事务收口报警。
 * 现场结论只有误报与真实火警；故障由设备上报确定，后续走隐患流程。
 */
@Schema(description = "工单完成请求")
@Data
public class WorkOrderFinishDTO {

    /** 由后台请求体或小程序路径参数写入 */
    @Schema(description = "主键 ID")
    private Long id;

    /** 0 误报，1 真实火警；故障由设备上报确定，不作为现场结论 */
    @NotNull(message = "处理结论不能为空")
    @Min(value = 0, message = "处理结论不合法")
    @Max(value = 1, message = "处理结论不合法")
    @Schema(description = "处理结果：0 误报，1 真实火警")
    private Integer result;

    @Schema(description = "备注")
    private String remark;
    @Schema(description = "现场照片 1")
    private String pic1;
    @Schema(description = "现场照片 2")
    private String pic2;
    @Schema(description = "现场照片 3")
    private String pic3;
}
