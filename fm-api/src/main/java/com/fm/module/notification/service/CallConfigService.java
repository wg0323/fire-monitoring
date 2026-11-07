package com.fm.module.notification.service;

import com.fm.module.notification.entity.SevenMoorConfig;

/**
 * 租户外呼配置服务：读表 + 本地缓存，保存后失效。
 * 未配置或 enabled=0 时返回 null，调用方按「未开通」处理。
 */
public interface CallConfigService {

    SevenMoorConfig getByTenant(Long tenantId);

    SevenMoorConfig current();

    void save(SevenMoorConfig config);
}
