package com.fm.module.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fm.common.context.TenantContext;
import com.fm.module.system.entity.SysOrg;
import com.fm.module.system.entity.SysRole;
import com.fm.module.system.entity.SysUserWorkArea;
import com.fm.module.system.entity.SysWorkAreaMerchant;
import com.fm.module.system.mapper.SysOrgMapper;
import com.fm.module.system.mapper.SysUserWorkAreaMapper;
import com.fm.module.system.mapper.SysWorkAreaMerchantMapper;
import com.fm.module.system.service.DataScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 数据范围实现：租户由租户插件过滤，工作区商户由本服务显式收窄。
 */
@Service
@RequiredArgsConstructor
public class DataScopeServiceImpl implements DataScopeService {

    private static final String USER_TYPE_PLATFORM = "PLATFORM";
    private static final String USER_TYPE_MERCHANT = "MERCHANT";

    private final SysOrgMapper orgMapper;
    private final SysUserWorkAreaMapper userWorkAreaMapper;
    private final SysWorkAreaMerchantMapper workAreaMerchantMapper;

    @Override
    public boolean isPlatform() {
        return USER_TYPE_PLATFORM.equals(TenantContext.getUserType());
    }

    @Override
    public Long tenantId() {
        return TenantContext.getTenantId();
    }

    @Override
    public List<Long> workAreaMerchantIds(Long userId) {
        List<Long> workAreaIds = workAreaIds(userId);
        if (workAreaIds.isEmpty()) {
            return Collections.emptyList();
        }
        return workAreaMerchantMapper.selectList(new LambdaQueryWrapper<SysWorkAreaMerchant>()
                        .in(SysWorkAreaMerchant::getWorkAreaId, workAreaIds))
                .stream().map(SysWorkAreaMerchant::getMerchantId).distinct().collect(Collectors.toList());
    }

    @Override
    public List<Long> workAreaIds(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return userWorkAreaMapper.selectList(
                        new LambdaQueryWrapper<SysUserWorkArea>().eq(SysUserWorkArea::getUserId, userId))
                .stream().map(SysUserWorkArea::getWorkAreaId).distinct().collect(Collectors.toList());
    }

    @Override
    public Long merchantId() {
        return TenantContext.getMerchantId();
    }

    @Override
    public List<Long> scopedMerchantIds() {
        if (USER_TYPE_MERCHANT.equals(TenantContext.getUserType())) {
            Long merchantId = TenantContext.getMerchantId();
            return merchantId == null ? Collections.emptyList() : Collections.singletonList(merchantId);
        }
        if (SysRole.SCOPE_WORK_AREA.equals(TenantContext.getDataScope())) {
            return workAreaMerchantIds(StpUtil.getLoginIdAsLong());
        }
        return null;
    }

    @Override
    public List<Long> authorizedTenantIds() {
        if (isPlatform()) {
            return orgMapper.selectList(new LambdaQueryWrapper<SysOrg>()
                            .eq(SysOrg::getType, SysOrg.TYPE_TENANT))
                    .stream().map(SysOrg::getId).collect(Collectors.toList());
        }
        return Collections.singletonList(TenantContext.getTenantId());
    }

    @Override
    public boolean canAccess(Long tenantId) {
        if (isPlatform()) {
            return true;
        }
        return tenantId != null && tenantId.equals(TenantContext.getTenantId());
    }

    @Override
    public <T> T withIgnoreTenant(Supplier<T> action) {
        boolean previous = TenantContext.isIgnoreTenant();
        TenantContext.setIgnoreTenant(true);
        try {
            return action.get();
        } finally {
            TenantContext.setIgnoreTenant(previous);
        }
    }
}
