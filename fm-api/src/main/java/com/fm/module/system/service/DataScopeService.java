package com.fm.module.system.service;

import java.util.List;
import java.util.function.Supplier;

/**
 * 数据范围：平台账号看全部租户，租户账号只看本租户，外勤再收窄到工作区商户。
 */
public interface DataScopeService {

    boolean isPlatform();

    Long tenantId();

    /**
     * 外勤所属工作区覆盖的商户 id。
     *
     * @param userId 用户 id
     * @return 商户 id 列表，未绑定工作区时为空
     */
    List<Long> workAreaMerchantIds(Long userId);

    /**
     * 用户绑定的工作区 id。
     *
     * @param userId 用户 id
     * @return 工作区 id 列表，未绑定时为空
     */
    List<Long> workAreaIds(Long userId);

    Long merchantId();

    /**
     * 当前登录用户可见的商户范围。
     *
     * @return null=不限商户；空列表=无可见商户；非空=限定商户
     */
    List<Long> scopedMerchantIds();

    List<Long> authorizedTenantIds();

    boolean canAccess(Long tenantId);

    <T> T withIgnoreTenant(Supplier<T> action);
}
