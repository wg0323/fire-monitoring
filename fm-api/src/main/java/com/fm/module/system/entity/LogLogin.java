package com.fm.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志。
 */
@Data
@TableName("log_login")
public class LogLogin {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long userId;
    private String userType;
    private LocalDateTime loginTime;
    private String loginIp;
    private Integer loginResult;
    private String failureReason;
}
