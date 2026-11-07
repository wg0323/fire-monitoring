package com.fm.module.danger.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 隐患主表：来源、责任人、期限与审核信息。
 * 状态流转：0 待指派 → 1 待整改 → 2 待审核 → 3 已完成；2 驳回后回落 1 待整改，
 * 驳回结论保留在 audit_result=4。
 */
@Data
@TableName("b_danger")
public class Danger {

    public static final int STATUS_PENDING_ASSIGN = 0;
    public static final int STATUS_PENDING_DEAL = 1;
    public static final int STATUS_PENDING_AUDIT = 2;
    public static final int STATUS_DONE = 3;
    public static final int STATUS_REJECTED = 4;

    public static final int SOURCE_SNAPSHOT = 0;
    public static final int SOURCE_INSPECTION = 1;
    public static final int SOURCE_ALARM = 2;

    public static final String ASSIGNEE_FIELD = "FIELD";
    public static final String ASSIGNEE_MERCHANT = "MERCHANT";

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long merchantId;
    private Long alarmId;
    private String dangerType;
    private String remark;
    private Integer sourceType;
    private Long sourceAlarmId;
    private LocalDateTime alarmTime;
    private LocalDateTime convertTime;
    private String assigneeType;
    private Long assigneeId;
    private String assigneeName;
    private Long workAreaId;
    private Long auditBy;
    private String auditByName;
    private LocalDateTime auditTime;
    private Integer auditResult;
    private String auditRemark;
    private BigDecimal lng;
    private BigDecimal lat;
    private String position;
    private String positionDetail;
    private String pic1;
    private String pic2;
    private String pic3;
    private Integer dangerLevel;
    private Integer dangerLevelFinal;
    private Integer status;
    private String createBy;
    private String assignBy;
    private LocalDateTime assignTime;
    private LocalDate dealLimit;
    private String dealBy;
    private LocalDateTime dealTime;
    private String checkBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
