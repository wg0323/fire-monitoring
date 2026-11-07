package com.fm.module.workorder.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工单列表/详情展示对象，附带来源报警与故障转隐患状态。
 */
@Data
@Schema(description = "工单列表/详情展示对象，附带来源报警与故障转隐患状态")
public class WorkOrderVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键ID")
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "租户主键ID")
    private Long tenantId;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "商户主键ID")
    private Long merchantId;
    @Schema(description = "商户名称")
    private String merchantName;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "报警主键ID")
    private Long alarmId;
    @Schema(description = "报警设备编号")
    private String alarmSn;
    @Schema(description = "报警时间")
    private LocalDateTime alarmTime;
    @Schema(description = "报警级别")
    private Integer alarmLevel;
    @Schema(description = "报警结果")
    private Integer alarmResult;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "工作区域主键ID")
    private Long workAreaId;
    @Schema(description = "工作区域名称")
    private String workAreaName;
    @Schema(description = "标题")
    private String title;
    @Schema(description = "内容")
    private String content;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "处理人主键ID")
    private Long handlerId;
    @Schema(description = "处理人名称")
    private String handlerName;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "字段 priority")
    private Integer priority;
    @Schema(description = "来源")
    private String source;
    @Schema(description = "accept时间")
    private LocalDateTime acceptTime;
    @Schema(description = "finish时间")
    private LocalDateTime finishTime;
    @Schema(description = "finish备注")
    private String finishRemark;
    @Schema(description = "字段 finishPic1")
    private String finishPic1;
    @Schema(description = "字段 finishPic2")
    private String finishPic2;
    @Schema(description = "字段 finishPic3")
    private String finishPic3;
    /** 故障结论的转隐患状态：PENDING 待转 / CONVERTED 已转 / RECOVERED 已自行恢复 / NONE 不适用 */
    @Schema(description = "故障结论的转隐患状态：PENDING 待转 / CONVERTED 已转 / RECOVERED 已自行恢复 / NON")
    private String dangerStatus;
    @Schema(description = "隐患remain秒")
    private Long dangerRemainSeconds;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "隐患主键ID")
    private Long dangerId;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
