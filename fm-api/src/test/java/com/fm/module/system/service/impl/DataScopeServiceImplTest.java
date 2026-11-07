package com.fm.module.system.service.impl;

import com.fm.common.context.TenantContext;
import com.fm.module.system.entity.SysOrg;
import com.fm.module.system.entity.SysUserWorkArea;
import com.fm.module.system.entity.SysWorkAreaMerchant;
import com.fm.module.system.mapper.SysOrgMapper;
import com.fm.module.system.mapper.SysUserWorkAreaMapper;
import com.fm.module.system.mapper.SysWorkAreaMerchantMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 数据范围测试：工作区商户集合与租户可访问性。
 */
class DataScopeServiceImplTest {

    private final SysOrgMapper orgMapper = mock(SysOrgMapper.class);
    private final SysUserWorkAreaMapper userWorkAreaMapper = mock(SysUserWorkAreaMapper.class);
    private final SysWorkAreaMerchantMapper workAreaMerchantMapper =
            mock(SysWorkAreaMerchantMapper.class);
    private final DataScopeServiceImpl service = new DataScopeServiceImpl(
            orgMapper, userWorkAreaMapper, workAreaMerchantMapper);

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void fieldMerchantsAreDedupedAcrossWorkAreas() {
        TenantContext.set(1L, "TENANT", "WORK_AREA", false);
        when(userWorkAreaMapper.selectList(any())).thenReturn(Arrays.asList(
                userWorkArea(1L), userWorkArea(2L)));
        when(workAreaMerchantMapper.selectList(any())).thenReturn(Arrays.asList(
                workAreaMerchant(10L), workAreaMerchant(11L), workAreaMerchant(10L)));

        List<Long> merchantIds = service.workAreaMerchantIds(100L);

        assertEquals(Arrays.asList(10L, 11L), merchantIds);
    }

    @Test
    void tenantCanOnlyAccessOwnTenant() {
        TenantContext.set(1L, "TENANT", "TENANT", false);

        assertTrue(service.canAccess(1L));
        assertFalse(service.canAccess(2L));
        assertFalse(service.canAccess(null));
        assertEquals(1L, service.tenantId());
    }

    @Test
    void platformCanAccessAllTenants() {
        TenantContext.set(0L, "PLATFORM", "ALL", true);
        SysOrg first = new SysOrg();
        first.setId(1L);
        first.setType(SysOrg.TYPE_TENANT);
        SysOrg second = new SysOrg();
        second.setId(2L);
        second.setType(SysOrg.TYPE_TENANT);
        when(orgMapper.selectList(any())).thenReturn(Arrays.asList(first, second));

        assertTrue(service.isPlatform());
        assertTrue(service.canAccess(2L));
        assertEquals(Arrays.asList(1L, 2L), service.authorizedTenantIds());
    }

    @Test
    void merchantScopeReturnsOwnMerchant() {
        TenantContext.set(1L, 7L, 9L, "MERCHANT", "MERCHANT", false);

        assertEquals(Collections.singletonList(9L), service.scopedMerchantIds());
    }

    @Test
    void tenantScopeIsUnrestricted() {
        TenantContext.set(1L, "TENANT", "TENANT", false);

        assertNull(service.scopedMerchantIds());
    }

    private SysUserWorkArea userWorkArea(Long workAreaId) {
        SysUserWorkArea relation = new SysUserWorkArea();
        relation.setTenantId(1L);
        relation.setUserId(100L);
        relation.setWorkAreaId(workAreaId);
        return relation;
    }

    private SysWorkAreaMerchant workAreaMerchant(Long merchantId) {
        SysWorkAreaMerchant relation = new SysWorkAreaMerchant();
        relation.setTenantId(1L);
        relation.setWorkAreaId(1L);
        relation.setMerchantId(merchantId);
        return relation;
    }
}
