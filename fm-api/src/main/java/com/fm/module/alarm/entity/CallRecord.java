package com.fm.module.alarm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 外呼通话记录：与只追加的 trace 分离，承接异步回调更新与录音同步。
 */
@Data
@TableName("b_call_record")
public class CallRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long alarmId;
    private Long traceId;
    private String calledNo;
    private String actionId;
    private String callState;
    private LocalDateTime callBegin;
    private LocalDateTime callEnd;
    private String monitorFileLocal;
    private String monitorFileRemote;
    @TableField("is_synced")
    private Integer synced;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}  
