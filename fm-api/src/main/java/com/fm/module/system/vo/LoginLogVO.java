package com.fm.module.system.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志展示对象。
 */
@Data
@Schema(description = "登录日志展示对象")
public class LoginLogVO {

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
    @Schema(description = "login时间")
    private LocalDateTime loginTime;
    @Schema(description = "loginIP地址")
    private String loginIp;
    @Schema(description = "login结果")
    private Integer loginResult;
    @Schema(description = "failure原因")
    private String failureReason;
}
