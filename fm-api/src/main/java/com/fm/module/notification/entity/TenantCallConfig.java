package com.fm.module.notification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 租户外呼配置：一个运营中心一个厂商一行，接口参数存 config JSON。
 */
@Data
@TableName("sys_tenant_call_config")
public class TenantCallConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String PROVIDER_SEVEN_MOOR = "seven_moor";

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private String provider;
    private String config;
    private Integer enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
