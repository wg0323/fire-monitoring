package com.fm.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户：平台 / 租户 / 商户三类账号统一存储。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long tenantId;
    private Long deptId;
    private String userType;
    private String userName;
    @JsonIgnore
    private String password;
    private String realName;
    private String phone;
    private Long merchantId;
    private String wxAppOpenid;
    private Integer status;
    private LocalDateTime lastLogin;
}
