package com.fm.module.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fm.common.context.TenantContext;
import com.fm.common.exception.BizException;
import com.fm.module.device.entity.Merchant;
import com.fm.module.device.mapper.MerchantMapper;
import com.fm.module.system.converter.UserConverter;
import com.fm.module.system.converter.WorkAreaConverter;
import com.fm.module.system.dto.WorkAreaQueryDTO;
import com.fm.module.system.dto.WorkAreaSaveDTO;
import com.fm.module.system.entity.SysOrg;
import com.fm.module.system.entity.SysUser;
import com.fm.module.system.entity.SysUserWorkArea;
import com.fm.module.system.entity.SysWorkArea;
import com.fm.module.system.entity.SysWorkAreaMerchant;
import com.fm.module.system.mapper.SysOrgMapper;
import com.fm.module.system.mapper.SysUserMapper;
import com.fm.module.system.mapper.SysUserWorkAreaMapper;
import com.fm.module.system.mapper.SysWorkAreaMapper;
import com.fm.module.system.mapper.SysWorkAreaMerchantMapper;
import com.fm.module.system.service.SysWorkAreaService;
import com.fm.module.system.support.AdminGuard;
import com.fm.module.system.vo.UserVO;
import com.fm.module.system.vo.WorkAreaVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 工作区维护：工作区及其用户、商户关联均按租户隔离。
 */
@Service
@RequiredArgsConstructor
public class SysWorkAreaServiceImpl extends ServiceImpl<SysWorkAreaMapper, SysWorkArea>
        implements SysWorkAreaService {

    private static final long PLATFORM_TENANT_ID = 0L;
    private static final String PLATFORM_NAME = "平台";

    private final SysUserWorkAreaMapper userWorkAreaMapper;
    private final SysWorkAreaMerchantMapper workAreaMerchantMapper;
    private final SysUserMapper userMapper;
    private final MerchantMapper merchantMapper;
    private final SysOrgMapper orgMapper;

    @Override
    public Page<WorkAreaVO> page(WorkAreaQueryDTO query) {
        AdminGuard.requireAdmin();
        Long tenantId = AdminGuard.isPlatform() ? query.getTenantId() : TenantContext.getTenantId();
        LambdaQueryWrapper<SysWorkArea> wrapper = new LambdaQueryWrapper<SysWorkArea>()
                .eq(tenantId != null, SysWorkArea::getTenantId, tenantId)
                .like(StringUtils.hasText(query.getKeyword()), SysWorkArea::getName, query.getKeyword())
                .orderByAsc(SysWorkArea::getId);
        Page<SysWorkArea> entityPage = scoped(() -> baseMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper));
        Page<WorkAreaVO> result = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<WorkAreaVO> records = entityPage.getRecords().stream()
                .map(WorkAreaConverter::toVO).collect(Collectors.toList());
        fillCounts(records);
        fillTenantNames(records);
        result.setRecords(records);
        return result;
    }

    @Override
    public List<WorkAreaVO> options(Long tenantId) {
        AdminGuard.requireAdmin();
        Long targetTenant = AdminGuard.isPlatform() ? tenantId : TenantContext.getTenantId();
        if (targetTenant == null) {
            return Collections.emptyList();
        }
        List<SysWorkArea> areas = scoped(() -> baseMapper.selectList(new LambdaQueryWrapper<SysWorkArea>()
                .eq(SysWorkArea::getTenantId, targetTenant)
                .orderByAsc(SysWorkArea::getId)));
        List<WorkAreaVO> records = areas.stream().map(WorkAreaConverter::toVO).collect(Collectors.toList());
        fillTenantNames(records);
        return records;
    }

    @Override
    public WorkAreaVO detail(Long id) {
        AdminGuard.requireAdmin();
        SysWorkArea area = requireAccessible(id);
        WorkAreaVO vo = WorkAreaConverter.toVO(area);
        List<WorkAreaVO> records = new ArrayList<>();
        records.add(vo);
        fillCounts(records);
        fillTenantNames(records);
        vo.setUserIds(userIds(id));
        vo.setMerchantIds(merchantIds(id));
        return vo;
    }

    @Override
    @Transactional
    public Long create(WorkAreaSaveDTO request) {
        AdminGuard.requireAdmin();
        validateName(request);
        Long tenantId = resolveTargetTenant(request.getTenantId());
        SysWorkArea area = new SysWorkArea();
        area.setTenantId(tenantId);
        area.setOrgId(tenantId);
        area.setName(request.getName());
        area.setRemark(request.getRemark());
        scoped(() -> baseMapper.insert(area));
        if (request.getUserIds() != null) {
            replaceUsers(area.getId(), tenantId, request.getUserIds());
        }
        if (request.getMerchantIds() != null) {
            replaceMerchants(area.getId(), tenantId, request.getMerchantIds());
        }
        return area.getId();
    }

    @Override
    @Transactional
    public void update(Long id, WorkAreaSaveDTO request) {
        AdminGuard.requireAdmin();
        SysWorkArea existing = requireAccessible(id);
        validateName(request);
        SysWorkArea area = new SysWorkArea();
        area.setId(id);
        area.setName(request.getName());
        area.setRemark(request.getRemark());
        scoped(() -> baseMapper.updateById(area));
        if (request.getUserIds() != null) {
            replaceUsers(id, existing.getTenantId(), request.getUserIds());
        }
        if (request.getMerchantIds() != null) {
            replaceMerchants(id, existing.getTenantId(), request.getMerchantIds());
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        AdminGuard.requireAdmin();
        requireAccessible(id);
        scopedVoid(() -> userWorkAreaMapper.delete(new LambdaQueryWrapper<SysUserWorkArea>()
                .eq(SysUserWorkArea::getWorkAreaId, id)));
        scopedVoid(() -> workAreaMerchantMapper.delete(new LambdaQueryWrapper<SysWorkAreaMerchant>()
                .eq(SysWorkAreaMerchant::getWorkAreaId, id)));
        scopedVoid(() -> baseMapper.deleteById(id));
    }

    @Override
    @Transactional
    public void bindMerchants(Long id, List<Long> merchantIds) {
        AdminGuard.requireAdmin();
        SysWorkArea area = requireAccessible(id);
        replaceMerchants(id, area.getTenantId(), merchantIds);
    }

    @Override
    @Transactional
    public void bindUsers(Long id, List<Long> userIds) {
        AdminGuard.requireAdmin();
        SysWorkArea area = requireAccessible(id);
        replaceUsers(id, area.getTenantId(), userIds);
    }

    @Override
    public List<UserVO> users(Long id) {
        AdminGuard.requireAdmin();
        requireAccessible(id);
        List<Long> userIds = userIds(id);
        if (userIds.isEmpty()) {
            return Collections.emptyList();
        }
        return scoped(() -> userMapper.selectBatchIds(userIds)).stream()
                .map(UserConverter::toVO).collect(Collectors.toList());
    }

    @Override
    public List<Long> merchantIds(Long id) {
        AdminGuard.requireAdmin();
        requireAccessible(id);
        return scoped(() -> workAreaMerchantMapper.selectList(new LambdaQueryWrapper<SysWorkAreaMerchant>()
                        .eq(SysWorkAreaMerchant::getWorkAreaId, id)))
                .stream().map(SysWorkAreaMerchant::getMerchantId).distinct().collect(Collectors.toList());
    }

    private List<Long> userIds(Long id) {
        return scoped(() -> userWorkAreaMapper.selectList(new LambdaQueryWrapper<SysUserWorkArea>()
                        .eq(SysUserWorkArea::getWorkAreaId, id)))
                .stream().map(SysUserWorkArea::getUserId).distinct().collect(Collectors.toList());
    }

    private SysWorkArea requireAccessible(Long id) {
        SysWorkArea area = scoped(() -> baseMapper.selectById(id));
        if (area == null) {
            throw new BizException("工作区不存在或无权操作");
        }
        if (!AdminGuard.isPlatform() && !area.getTenantId().equals(TenantContext.getTenantId())) {
            throw new BizException(403, "不能维护其他租户的工作区");
        }
        return area;
    }

    private Long resolveTargetTenant(Long requestedTenantId) {
        if (!AdminGuard.isPlatform()) {
            if (requestedTenantId != null && !requestedTenantId.equals(TenantContext.getTenantId())) {
                throw new BizException(403, "不能创建其他租户的工作区");
            }
            return TenantContext.getTenantId();
        }
        if (requestedTenantId == null) {
            throw new BizException("请选择所属运营中心");
        }
        SysOrg org = orgMapper.selectById(requestedTenantId);
        if (org == null || !SysOrg.TYPE_TENANT.equals(org.getType()) || org.getStatus() == null
                || org.getStatus() != 1) {
            throw new BizException("所属运营中心不存在或已禁用");
        }
        return requestedTenantId;
    }

    private void validateName(WorkAreaSaveDTO request) {
        if (!StringUtils.hasText(request.getName())) {
            throw new BizException("工作区名称不能为空");
        }
    }

    private void replaceUsers(Long workAreaId, Long tenantId, List<Long> userIds) {
        List<Long> targetIds = userIds == null ? Collections.emptyList()
                : userIds.stream().distinct().collect(Collectors.toList());
        validateUsers(tenantId, targetIds);
        scopedVoid(() -> userWorkAreaMapper.delete(new LambdaQueryWrapper<SysUserWorkArea>()
                .eq(SysUserWorkArea::getWorkAreaId, workAreaId)));
        for (Long userId : targetIds) {
            SysUserWorkArea relation = new SysUserWorkArea();
            relation.setTenantId(tenantId);
            relation.setWorkAreaId(workAreaId);
            relation.setUserId(userId);
            scopedVoid(() -> userWorkAreaMapper.insert(relation));
        }
    }

    private void replaceMerchants(Long workAreaId, Long tenantId, List<Long> merchantIds) {
        List<Long> targetIds = merchantIds == null ? Collections.emptyList()
                : merchantIds.stream().distinct().collect(Collectors.toList());
        validateMerchants(tenantId, targetIds);
        scopedVoid(() -> workAreaMerchantMapper.delete(new LambdaQueryWrapper<SysWorkAreaMerchant>()
                .eq(SysWorkAreaMerchant::getWorkAreaId, workAreaId)));
        for (Long merchantId : targetIds) {
            SysWorkAreaMerchant relation = new SysWorkAreaMerchant();
            relation.setTenantId(tenantId);
            relation.setWorkAreaId(workAreaId);
            relation.setMerchantId(merchantId);
            scopedVoid(() -> workAreaMerchantMapper.insert(relation));
        }
    }

    private void validateUsers(Long tenantId, List<Long> userIds) {
        if (userIds.isEmpty()) {
            return;
        }
        List<SysUser> users = scoped(() -> userMapper.selectBatchIds(userIds));
        boolean invalid = users.size() != userIds.size()
                || users.stream().anyMatch(user -> !tenantId.equals(user.getTenantId()));
        if (invalid) {
            throw new BizException("工作区成员必须属于同一运营中心");
        }
    }

    private void validateMerchants(Long tenantId, List<Long> merchantIds) {
        if (merchantIds.isEmpty()) {
            return;
        }
        List<Merchant> merchants = scoped(() -> merchantMapper.selectBatchIds(merchantIds));
        boolean invalid = merchants.size() != merchantIds.size()
                || merchants.stream().anyMatch(merchant -> !tenantId.equals(merchant.getTenantId()));
        if (invalid) {
            throw new BizException("工作区商户必须属于同一运营中心");
        }
    }

    private void fillCounts(List<WorkAreaVO> records) {
        List<Long> areaIds = records.stream().map(WorkAreaVO::getId).collect(Collectors.toList());
        if (areaIds.isEmpty()) {
            return;
        }
        Map<Long, Long> userCounts = scoped(() -> userWorkAreaMapper.selectList(
                        new LambdaQueryWrapper<SysUserWorkArea>()
                                .in(SysUserWorkArea::getWorkAreaId, areaIds)))
                .stream().collect(Collectors.groupingBy(SysUserWorkArea::getWorkAreaId, Collectors.counting()));
        Map<Long, Long> merchantCounts = scoped(() -> workAreaMerchantMapper.selectList(
                        new LambdaQueryWrapper<SysWorkAreaMerchant>()
                                .in(SysWorkAreaMerchant::getWorkAreaId, areaIds)))
                .stream().collect(Collectors.groupingBy(
                        SysWorkAreaMerchant::getWorkAreaId, Collectors.counting()));
        for (WorkAreaVO record : records) {
            record.setUserCount(userCounts.getOrDefault(record.getId(), 0L));
            record.setMerchantCount(merchantCounts.getOrDefault(record.getId(), 0L));
        }
    }

    private void fillTenantNames(List<WorkAreaVO> records) {
        List<Long> tenantIds = records.stream().map(WorkAreaVO::getTenantId)
                .filter(id -> id != null && id != PLATFORM_TENANT_ID).distinct()
                .collect(Collectors.toList());
        Map<Long, String> names = new HashMap<>();
        if (!tenantIds.isEmpty()) {
            names = orgMapper.selectBatchIds(tenantIds).stream()
                    .collect(Collectors.toMap(SysOrg::getId, SysOrg::getName, (a, b) -> a));
        }
        for (WorkAreaVO record : records) {
            Long tenantId = record.getTenantId();
            record.setTenantName(tenantId == null || tenantId == PLATFORM_TENANT_ID
                    ? PLATFORM_NAME : names.get(tenantId));
        }
    }

    private <T> T scoped(Supplier<T> action) {
        if (!AdminGuard.isPlatform()) {
            return action.get();
        }
        return withIgnoreTenant(action);
    }

    private void scopedVoid(Runnable action) {
        if (!AdminGuard.isPlatform()) {
            action.run();
            return;
        }
        withIgnoreTenant(() -> {
            action.run();
            return null;
        });
    }

    private <T> T withIgnoreTenant(Supplier<T> action) {
        boolean previous = TenantContext.isIgnoreTenant();
        TenantContext.setIgnoreTenant(true);
        try {
            return action.get();
        } finally {
            TenantContext.setIgnoreTenant(previous);
        }
    }
}
