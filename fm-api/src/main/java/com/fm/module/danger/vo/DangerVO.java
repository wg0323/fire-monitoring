package com.fm.module.danger.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 隐患列表/详情展示对象。
 */
@Data
@Schema(description = "隐患列表/详情展示对象")
public class DangerVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键ID")
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "商户主键ID")
    private Long merchantId;
    @Schema(description = "商户名称")
    private String merchantName;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "报警主键ID")
    private Long alarmId;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "来源报警主键ID")
    private Long sourceAlarmId;
    @Schema(description = "来源类型")
    private Integer sourceType;
    @Schema(description = "隐患类型")
    private String dangerType;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "位置")
    private String position;
    @Schema(description = "位置详情")
    private String positionDetail;
    @Schema(description = "字段 pic1")
    private String pic1;
    @Schema(description = "字段 pic2")
    private String pic2;
    @Schema(description = "字段 pic3")
    private String pic3;
    @Schema(description = "隐患级别")
    private Integer dangerLevel;
    @Schema(description = "隐患级别final")
    private Integer dangerLevelFinal;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "创建by")
    private String createBy;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "处理人类型")
    private String assigneeType;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "处理人主键ID")
    private Long assigneeId;
    @Schema(description = "处理人名称")
    private String assigneeName;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "工作区域主键ID")
    private Long workAreaId;
    @Schema(description = "工作区域名称")
    private String workAreaName;
    @Schema(description = "报警时间")
    private LocalDateTime alarmTime;
    @Schema(description = "convert时间")
    private LocalDateTime convertTime;
    @Schema(description = "指派by")
    private String assignBy;
    @Schema(description = "指派时间")
    private LocalDateTime assignTime;
    @Schema(description = "处置条数")
    private LocalDate dealLimit;
    @Schema(description = "处置by")
    private String dealBy;
    @Schema(description = "处置时间")
    private LocalDateTime dealTime;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "审核by")
    private Long auditBy;
    @Schema(description = "审核名称")
    private String auditByName;
    @Schema(description = "审核时间")
    private LocalDateTime auditTime;
    @Schema(description = "审核结果")
    private Integer auditResult;
    @Schema(description = "审核备注")
    private String auditRemark;
    @Schema(description = "最后处置用户名称")
    private String lastDealUserName;
    @Schema(description = "最后处置时间")
    private LocalDateTime lastDealTime;
    @Schema(description = "最后处置备注")
    private String lastDealRemark;
    @Schema(description = "最后处置pic1")
    private String lastDealPic1;
    @Schema(description = "最后处置pic2")
    private String lastDealPic2;
    @Schema(description = "最后处置pic3")
    private String lastDealPic3;
}
