package com.fm.module.notification.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内消息展示对象。
 */
@Data
@Schema(description = "站内消息展示对象")
public class MpNotificationVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键ID")
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "租户主键ID")
    private Long tenantId;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "用户主键ID")
    private Long userId;
    @Schema(description = "用户名称")
    private String userName;
    @Schema(description = "真实名称")
    private String realName;
    @Schema(description = "用户类型")
    private String userType;
    @Schema(description = "类型")
    private Integer type;
    @Schema(description = "内容")
    private String content;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "related主键ID")
    private Long relatedId;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "消息时间")
    private LocalDateTime messageTime;
    @Schema(description = "通道")
    private String channel;
    @Schema(description = "send状态")
    private Integer sendStatus;
    @Schema(description = "fail原因")
    private String failReason;
}
