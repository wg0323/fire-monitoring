package com.fm.module.alarm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 报警追踪：时间线的一条记录，多态字段进 detail JSON。
 */
@Data
@TableName("b_fire_alarm_trace")
public class FireAlarmTrace implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int TYPE_DEVICE = 0;
    public static final int TYPE_NOTIFY = 1;
    public static final int TYPE_CALL_ALARM = 2;
    public static final int TYPE_CALL_VERIFY = 3;
    public static final int TYPE_DISPATCH = 4;
    public static final int TYPE_VERIFY = 5;
    public static final int TYPE_RECOVER = 6;
    public static final int TYPE_MERCHANT_HANDLE = 7;
    public static final int TYPE_CONVERT_DANGER = 8;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long alarmId;
    private Integer traceType;
    private Integer result;
    private String content;
    private String detail;
    private String messageId;
    private LocalDateTime createTime;
}
