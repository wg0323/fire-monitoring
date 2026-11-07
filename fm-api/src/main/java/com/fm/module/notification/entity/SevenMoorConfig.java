package com.fm.module.notification.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 七陌外呼接口参数，序列化后存入 sys_tenant_call_config.config。
 */
@Data
public class SevenMoorConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private String accountId;
    private String apiSecret;
    private String host;
    private String fromExten;
}
