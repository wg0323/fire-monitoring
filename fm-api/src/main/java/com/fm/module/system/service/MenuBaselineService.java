package com.fm.module.system.service;

/**
 * 平台菜单基线维护。
 */
public interface MenuBaselineService {

    /**
     * 幂等写入 tenant_id=0 的完整基线。
     *
     * @return 新增条数
     */
    int ensureBaseline();

    /**
     * 为新租户复制一次基线，复制后与基线脱钩。
     *
     * @param tenantId 新租户 id
     * @return 复制条数
     */
    int copyBaseline(Long tenantId);
}
