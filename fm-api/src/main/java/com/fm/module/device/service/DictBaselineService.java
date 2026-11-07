package com.fm.module.device.service;

/**
 * 租户字典基线：新建运营中心时从平台基线复制一份，租户维护副本、与基线解耦。
 */
public interface DictBaselineService {

    /**
     * 为新租户复制平台字典，租户已有字典时不重复复制。
     *
     * @return 复制的字典项总数
     */
    int provisionTenantDict(Long tenantId);

    /** 删除运营中心时清理其字典 */
    void removeTenantDict(Long tenantId);
}
