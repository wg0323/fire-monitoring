package com.fm.module.alarm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 报警主表：设备上报快照 + 收口、商户处置、派单与故障转隐患状态。
 */
@Data
@TableName("b_fire_alarm")
public class FireAlarm implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_HANDLED = 1;

    public static final int VERIFY_PENDING = 0;
    public static final int VERIFY_DOING = 1;
    public static final int VERIFY_DONE = 2;

    public static final int RESULT_FALSE = 0;
    public static final int RESULT_TRUE = 1;
    public static final int RESULT_FAULT = 2;

    public static final String SOURCE_DUTY = "DUTY";
    public static final String SOURCE_MERCHANT = "MERCHANT";
    public static final String SOURCE_FIELD = "FIELD";
    public static final String SOURCE_ADMIN = "ADMIN";
    public static final String SOURCE_SYSTEM = "SYSTEM";

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private String messageId;
    private String sn;
    private String deviceType;
    private Long merchantId;
    private String position;
    private LocalDateTime alarmTime;
    private String alarmType;
    private Integer alarmLevel;
    private Integer eventCode;
    private Integer loopNo;
    private Integer nodeNo;
    private Integer channelId;
    private String reportData;
    private String rawData;
    private String snapshotUrl;
    private Integer status;
    private Integer result;
    private Integer verifyStatus;
    private Integer merchantHandleResult;
    private LocalDateTime merchantHandleTime;
    private Long merchantHandleBy;
    private String handleSource;
    private Long handlerId;
    private String handlerName;
    private Long workAreaId;
    private Long dangerId;
    private LocalDateTime finalizeTime;
    private LocalDateTime dangerConvertDeadline;
    private LocalDateTime recoverTime;
    private String dispatchUser;
    private LocalDateTime dispatchTime;
    private String dispatchRemark;
    private Long opUserId;
    private String opUserName;
    private String addRemark;
    private String addPic1;
    private String addPic2;
    private String addPic3;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
