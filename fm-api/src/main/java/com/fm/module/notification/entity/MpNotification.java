package com.fm.module.notification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 站内消息：报警、隐患与系统通知。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("b_mp_notification")
public class MpNotification extends BaseEntity {

    private static final long serialVersionUID = 1L;

    public static final int TYPE_ALARM = 1;
    public static final int TYPE_FAULT = 2;
    public static final int TYPE_DANGER = 3;
    public static final int TYPE_SYSTEM = 4;

    public static final int STATUS_UNREAD = 0;
    public static final int STATUS_READ = 1;

    private Long tenantId;
    private Long userId;
    private String userType;
    private Integer type;
    private String content;
    private Long relatedId;
    private Integer status;
    private LocalDateTime messageTime;
    private String channel;
    private Integer sendStatus;
    private String failReason;
}
