package com.fm.module.inspection.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fm.common.context.TenantContext;
import com.fm.common.exception.BizException;
import com.fm.module.danger.dto.DangerSaveDTO;
import com.fm.module.danger.service.DangerService;
import com.fm.module.device.entity.Merchant;
import com.fm.module.device.mapper.MerchantMapper;
import com.fm.module.inspection.converter.PlanConverter;
import com.fm.module.inspection.dto.PlanMerchantQueryDTO;
import com.fm.module.inspection.dto.PlanQueryDTO;
import com.fm.module.inspection.dto.PlanResultSaveDTO;
import com.fm.module.inspection.dto.PlanSaveDTO;
import com.fm.module.inspection.entity.Plan;
import com.fm.module.inspection.entity.PlanResult;
import com.fm.module.inspection.mapper.PlanMapper;
import com.fm.module.inspection.mapper.PlanMerchantMapper;
import com.fm.module.inspection.mapper.PlanResultMapper;
import com.fm.module.inspection.service.PlanService;
import com.fm.module.inspection.vo.PlanMerchantVO;
import com.fm.module.inspection.vo.PlanResultVO;
import com.fm.module.inspection.vo.PlanVO;
import com.fm.module.miniapp.MpConstants;
import com.fm.module.system.entity.SysRole;
import com.fm.module.system.entity.SysUser;
import com.fm.module.system.entity.SysWorkArea;
import com.fm.module.system.entity.SysWorkAreaMerchant;
import com.fm.module.system.mapper.SysUserMapper;
import com.fm.module.system.mapper.SysWorkAreaMapper;
import com.fm.module.system.mapper.SysWorkAreaMerchantMapper;
import com.fm.module.system.service.DataScopeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 巡查计划：按工作区创建，外勤只看所属工作区；巡查发现隐患时同步生成商户整改隐患。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanServiceImpl extends ServiceImpl<PlanMapper, Plan> implements PlanService {

    private static final long DEFAULT_PAGE_SIZE = 10L;
    private static final long MAX_PAGE_SIZE = 100L;
    private static final int STATUS_NOT_STARTED = 0;
    private static final int STATUS_RUNNING = 1;
    private static final int STATUS_ENDED = 2;
    private static final int MERCHANT_ENABLED = 1;

    private final PlanResultMapper planResultMapper;
    private final PlanMerchantMapper planMerchantMapper;
    private final MerchantMapper merchantMapper;
    private final SysUserMapper userMapper;
    private final SysWorkAreaMapper workAreaMapper;
    private final SysWorkAreaMerchantMapper workAreaMerchantMapper;
    private final DataScopeService dataScopeService;
    private final DangerService dangerService;

    @Override
    public Page<PlanVO> page(PlanQueryDTO query) {
        long pageNum = query.getPageNum() < 1 ? 1 : query.getPageNum();
        long pageSize = query.getPageSize() < 1
                ? DEFAULT_PAGE_SIZE : Math.min(query.getPageSize(), MAX_PAGE_SIZE);
        LambdaQueryWrapper<Plan> wrapper = new LambdaQueryWrapper<Plan>()
                .likeRight(StringUtils.hasText(query.getName()), Plan::getName, query.getName())
                .eq(query.getWorkAreaId() != null, Plan::getWorkAreaId, query.getWorkAreaId())
                .orderByDesc(Plan::getStartDate)
                .orderByDesc(Plan::getId);
        Page<Plan> page;
        if (dataScopeService.isPlatform()) {
            List<Long> tenantIds = dataScopeService.authorizedTenantIds();
            if (tenantIds.isEmpty()) {
                return new Page<>(pageNum, pageSize);
            }
            wrapper.in(Plan::getTenantId, tenantIds);
            page = dataScopeService.withIgnoreTenant(() -> baseMapper.selectPage(
                    new Page<>(pageNum, pageSize), wrapper));
        } else if (SysRole.SCOPE_WORK_AREA.equals(TenantContext.getDataScope())) {
            List<Long> workAreaIds = dataScopeService.workAreaIds(currentUserId());
            if (workAreaIds.isEmpty()) {
                return new Page<>(pageNum, pageSize);
            }
            wrapper.in(Plan::getWorkAreaId, workAreaIds);
            page = baseMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        } else {
            page = baseMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        }
        Page<PlanVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(toVOList(page.getRecords()));
        return result;
    }

    @Override
    public long countOngoingUnfinished() {
        if (!isMpInspectionUser()) {
            return 0L;
        }
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<Plan> wrapper = new LambdaQueryWrapper<Plan>()
                .le(Plan::getStartDate, today)
                .ge(Plan::getEndDate, today);
        return scopedPlans(wrapper).stream().filter(this::hasUnfinishedMerchant).count();
    }

    /**
     * 与小程序巡查入口一致：租户账号且为租户管理员或外勤。
     */
    private boolean isMpInspectionUser() {
        if (!MpConstants.USER_TYPE_TENANT.equals(TenantContext.getUserType())) {
            return false;
        }
        return currentUserIsTenantAdmin() || SysRole.SCOPE_WORK_AREA.equals(TenantContext.getDataScope());
    }

    private List<Plan> scopedPlans(LambdaQueryWrapper<Plan> wrapper) {
        if (dataScopeService.isPlatform()) {
            List<Long> tenantIds = dataScopeService.authorizedTenantIds();
            if (tenantIds.isEmpty()) {
                return Collections.emptyList();
            }
            wrapper.in(Plan::getTenantId, tenantIds);
            return dataScopeService.withIgnoreTenant(() -> baseMapper.selectList(wrapper));
        }
        if (SysRole.SCOPE_WORK_AREA.equals(TenantContext.getDataScope())) {
            List<Long> workAreaIds = dataScopeService.workAreaIds(currentUserId());
            if (workAreaIds.isEmpty()) {
                return Collections.emptyList();
            }
            wrapper.in(Plan::getWorkAreaId, workAreaIds);
        }
        return baseMapper.selectList(wrapper);
    }

    private boolean hasUnfinishedMerchant(Plan plan) {
        Long total = countWorkAreaMerchants(plan.getWorkAreaId());
        return total != null && total > planResultMapper.countFinished(plan.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(PlanSaveDTO request) {
        ensureTenantAdmin();
        validate(request, true);
        SysWorkArea workArea = requireWorkArea(request.getWorkAreaId());
        Plan plan = new Plan();
        plan.setTenantId(TenantContext.getTenantId());
        plan.setWorkAreaId(workArea.getId());
        plan.setName(request.getName());
        plan.setStartDate(request.getStartDate());
        plan.setEndDate(request.getEndDate());
        plan.setRemark(request.getRemark());
        plan.setCreateBy(currentUserId());
        baseMapper.insert(plan);
        return plan.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(PlanSaveDTO request) {
        ensureTenantAdmin();
        if (request.getId() == null) {
            throw new BizException("计划 id 不能为空");
        }
        Plan existing = requirePlan(request.getId());
        validate(request, false);
        SysWorkArea workArea = requireWorkArea(request.getWorkAreaId());
        if (!Objects.equals(existing.getWorkAreaId(), workArea.getId()) && hasResult(existing.getId())) {
            throw new BizException("该计划已有巡查记录，不能更换工作区");
        }
        baseMapper.update(null, new LambdaUpdateWrapper<Plan>()
                .eq(Plan::getId, existing.getId())
                .set(Plan::getWorkAreaId, workArea.getId())
                .set(Plan::getName, request.getName())
                .set(Plan::getStartDate, request.getStartDate())
                .set(Plan::getEndDate, request.getEndDate())
                .set(Plan::getRemark, request.getRemark())
                .set(Plan::getUpdateBy, currentUserId())
                .set(Plan::getUpdateTime, LocalDateTime.now()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ensureTenantAdmin();
        Plan plan = requirePlan(id);
        if (!LocalDate.now().isBefore(plan.getStartDate())) {
            throw new BizException("只允许删除未开始的计划");
        }
        if (hasResult(id)) {
            throw new BizException("该计划已有巡查记录，不能删除");
        }
        baseMapper.deleteById(id);
    }

    @Override
    public Page<PlanResultVO> results(Long planId, long pageNum, long pageSize) {
        requirePlan(planId);
        long safePageNum = pageNum < 1 ? 1 : pageNum;
        long safePageSize = pageSize < 1 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        Page<PlanResult> page = dataScopeService.isPlatform()
                ? dataScopeService.withIgnoreTenant(() -> planResultMapper.selectPage(
                        new Page<>(safePageNum, safePageSize), resultWrapper(planId)))
                : planResultMapper.selectPage(new Page<>(safePageNum, safePageSize), resultWrapper(planId));
        Set<Long> merchantIds = page.getRecords().stream().map(PlanResult::getMerchantId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> merchantNames = merchantNames(merchantIds);
        Page<PlanResultVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream()
                .map(item -> PlanConverter.toResultVO(item, merchantNames.get(item.getMerchantId())))
                .collect(Collectors.toList()));
        return result;
    }

    @Override
    public PlanVO detail(Long id) {
        Plan plan = requirePlan(id);
        return toVOList(Collections.singletonList(plan)).get(0);
    }

    @Override
    public List<PlanMerchantVO> merchants(Long planId) {
        Plan plan = requirePlan(planId);
        LambdaQueryWrapper<SysWorkAreaMerchant> relationWrapper = new LambdaQueryWrapper<SysWorkAreaMerchant>()
                .eq(SysWorkAreaMerchant::getWorkAreaId, plan.getWorkAreaId());
        List<SysWorkAreaMerchant> relations = dataScopeService.isPlatform()
                ? dataScopeService.withIgnoreTenant(() -> workAreaMerchantMapper.selectList(relationWrapper))
                : workAreaMerchantMapper.selectList(relationWrapper);
        List<Long> merchantIds = relations.stream().map(SysWorkAreaMerchant::getMerchantId)
                .distinct().collect(Collectors.toList());
        if (merchantIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Merchant> merchants = dataScopeService.isPlatform()
                ? dataScopeService.withIgnoreTenant(() -> merchantMapper.selectBatchIds(merchantIds))
                : merchantMapper.selectBatchIds(merchantIds);
        Map<Long, PlanResult> latest = latestResultMap(planId);
        return merchants.stream()
                .filter(merchant -> Integer.valueOf(MERCHANT_ENABLED).equals(merchant.getStatus()))
                .map(merchant -> PlanConverter.toMerchantVO(merchant, latest.get(merchant.getId())))
                .collect(Collectors.toList());
    }

    @Override
    public Page<PlanMerchantVO> merchantPage(Long planId, PlanMerchantQueryDTO query) {
        Plan plan = requirePlan(planId);
        long pageNum = query.getPageNum() < 1 ? 1 : query.getPageNum();
        long pageSize = query.getPageSize() < 1
                ? DEFAULT_PAGE_SIZE : Math.min(query.getPageSize(), MAX_PAGE_SIZE);
        return planMerchantMapper.selectPageByPlan(new Page<>(pageNum, pageSize), plan.getId(),
                plan.getTenantId(), plan.getWorkAreaId(), query.getName(), query.getPatrolStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveResult(PlanResultSaveDTO request, Long userId) {
        Plan plan = requirePlan(request.getPlanId());
        Merchant merchant = requirePlanMerchant(plan, request.getMerchantId());
        validateResult(request);
        Long dangerId = null;
        if (Integer.valueOf(PlanResult.RESULT_DANGER).equals(request.getResult())) {
            dangerId = dangerService.reportInspection(buildDangerRequest(merchant, request));
        }
        LocalDateTime now = LocalDateTime.now();
        PlanResult result = new PlanResult();
        result.setTenantId(plan.getTenantId());
        result.setPlanId(plan.getId());
        result.setMerchantId(merchant.getId());
        result.setUserId(userId);
        result.setWorkAreaId(plan.getWorkAreaId());
        result.setDangerId(dangerId);
        result.setPatrolBy(userName(userId));
        result.setPatrolTime(now);
        result.setResult(request.getResult());
        result.setRemark(request.getRemark());
        result.setPic1(request.getPic1());
        result.setPic2(request.getPic2());
        result.setPic3(request.getPic3());
        result.setLng(request.getLng());
        result.setLat(request.getLat());
        result.setCreateBy(userId);
        planResultMapper.insert(result);
        log.info("plan result submitted, planId={}, merchantId={}, result={}, dangerId={}",
                plan.getId(), merchant.getId(), request.getResult(), dangerId);
    }

    private LambdaQueryWrapper<PlanResult> resultWrapper(Long planId) {
        return new LambdaQueryWrapper<PlanResult>()
                .eq(PlanResult::getPlanId, planId)
                .orderByDesc(PlanResult::getPatrolTime)
                .orderByDesc(PlanResult::getId);
    }

    private boolean hasResult(Long planId) {
        Long count = planResultMapper.selectCount(new LambdaQueryWrapper<PlanResult>()
                .eq(PlanResult::getPlanId, planId));
        return count != null && count > 0;
    }

    private DangerSaveDTO buildDangerRequest(Merchant merchant, PlanResultSaveDTO request) {
        DangerSaveDTO danger = new DangerSaveDTO();
        danger.setMerchantId(merchant.getId());
        danger.setDangerType(request.getDangerType());
        danger.setRemark(request.getRemark());
        danger.setPosition(merchant.getAddr());
        danger.setLng(request.getLng());
        danger.setLat(request.getLat());
        danger.setPic1(request.getPic1());
        danger.setPic2(request.getPic2());
        danger.setPic3(request.getPic3());
        return danger;
    }

    private Plan requirePlan(Long id) {
        if (id == null) {
            throw new BizException("巡查计划不存在");
        }
        Plan plan = dataScopeService.isPlatform()
                ? dataScopeService.withIgnoreTenant(() -> baseMapper.selectById(id))
                : baseMapper.selectById(id);
        if (plan == null) {
            throw new BizException("巡查计划不存在");
        }
        if (dataScopeService.isPlatform()) {
            if (!dataScopeService.canAccess(plan.getTenantId())) {
                throw new BizException("巡查计划不存在");
            }
            return plan;
        }
        if (SysRole.SCOPE_WORK_AREA.equals(TenantContext.getDataScope())) {
            List<Long> workAreaIds = dataScopeService.workAreaIds(currentUserId());
            if (!workAreaIds.contains(plan.getWorkAreaId())) {
                throw new BizException("巡查计划不存在");
            }
        }
        return plan;
    }

    private SysWorkArea requireWorkArea(Long workAreaId) {
        SysWorkArea workArea = workAreaId == null ? null : workAreaMapper.selectById(workAreaId);
        if (workArea == null) {
            throw new BizException("工作区不存在");
        }
        if (!dataScopeService.isPlatform()
                && !Objects.equals(workArea.getTenantId(), TenantContext.getTenantId())) {
            throw new BizException("工作区不存在");
        }
        return workArea;
    }

    private Merchant requirePlanMerchant(Plan plan, Long merchantId) {
        Merchant merchant = merchantId == null ? null : merchantMapper.selectById(merchantId);
        if (merchant == null || !Objects.equals(plan.getTenantId(), merchant.getTenantId())) {
            throw new BizException("商户不存在");
        }
        Long bound = workAreaMerchantMapper.selectCount(new LambdaQueryWrapper<SysWorkAreaMerchant>()
                .eq(SysWorkAreaMerchant::getWorkAreaId, plan.getWorkAreaId())
                .eq(SysWorkAreaMerchant::getMerchantId, merchantId));
        if (bound == null || bound == 0) {
            throw new BizException("商户不属于该计划工作区");
        }
        return merchant;
    }

    private void validate(PlanSaveDTO request, boolean create) {
        if (!StringUtils.hasText(request.getName())) {
            throw new BizException("计划名称不能为空");
        }
        if (request.getWorkAreaId() == null) {
            throw new BizException("请选择工作区");
        }
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new BizException("请选择计划起止日期");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BizException("结束日期不能早于开始日期");
        }
        if (create && request.getStartDate().isBefore(LocalDate.now())) {
            throw new BizException("开始日期不能早于今天");
        }
    }

    private void validateResult(PlanResultSaveDTO request) {
        if (!Integer.valueOf(PlanResult.RESULT_NORMAL).equals(request.getResult())
                && !Integer.valueOf(PlanResult.RESULT_DANGER).equals(request.getResult())) {
            throw new BizException("巡查结果不合法");
        }
        if (Integer.valueOf(PlanResult.RESULT_DANGER).equals(request.getResult())
                && (!StringUtils.hasText(request.getDangerType()) || !StringUtils.hasText(request.getRemark()))) {
            throw new BizException("发现隐患时必须填写隐患类型与描述");
        }
    }

    private List<PlanVO> toVOList(List<Plan> list) {
        if (list.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> workAreaIds = list.stream().map(Plan::getWorkAreaId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> workAreaNames = workAreaNames(workAreaIds);
        Set<Long> createUserIds = list.stream().map(Plan::getCreateBy)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> userNames = userNames(createUserIds);
        return list.stream().map(plan -> PlanConverter.toVO(plan,
                workAreaNames.get(plan.getWorkAreaId()), userNames.get(plan.getCreateBy()), resolveStatus(plan),
                countWorkAreaMerchants(plan.getWorkAreaId()), planResultMapper.countFinished(plan.getId()),
                planResultMapper.countDanger(plan.getId())))
                .collect(Collectors.toList());
    }

    private Long countWorkAreaMerchants(Long workAreaId) {
        if (workAreaId == null) {
            return 0L;
        }
        LambdaQueryWrapper<SysWorkAreaMerchant> relationWrapper = new LambdaQueryWrapper<SysWorkAreaMerchant>()
                .eq(SysWorkAreaMerchant::getWorkAreaId, workAreaId);
        List<SysWorkAreaMerchant> relations = dataScopeService.isPlatform()
                ? dataScopeService.withIgnoreTenant(() -> workAreaMerchantMapper.selectList(relationWrapper))
                : workAreaMerchantMapper.selectList(relationWrapper);
        List<Long> merchantIds = relations.stream().map(SysWorkAreaMerchant::getMerchantId)
                .distinct().collect(Collectors.toList());
        if (merchantIds.isEmpty()) {
            return 0L;
        }
        LambdaQueryWrapper<Merchant> merchantWrapper = new LambdaQueryWrapper<Merchant>()
                .in(Merchant::getId, merchantIds)
                .eq(Merchant::getStatus, MERCHANT_ENABLED);
        Long count = dataScopeService.isPlatform()
                ? dataScopeService.withIgnoreTenant(() -> merchantMapper.selectCount(merchantWrapper))
                : merchantMapper.selectCount(merchantWrapper);
        return count == null ? 0L : count;
    }

    private Map<Long, PlanResult> latestResultMap(Long planId) {
        List<PlanResult> results = dataScopeService.isPlatform()
                ? dataScopeService.withIgnoreTenant(() -> planResultMapper.selectList(resultWrapper(planId)))
                : planResultMapper.selectList(resultWrapper(planId));
        Map<Long, PlanResult> latest = new HashMap<>(results.size());
        results.forEach(item -> latest.putIfAbsent(item.getMerchantId(), item));
        return latest;
    }

    private Map<Long, String> workAreaNames(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysWorkArea> areas = dataScopeService.isPlatform()
                ? dataScopeService.withIgnoreTenant(() -> workAreaMapper.selectBatchIds(ids))
                : workAreaMapper.selectBatchIds(ids);
        return areas.stream()
                .collect(Collectors.toMap(SysWorkArea::getId, SysWorkArea::getName, (a, b) -> a));
    }

    private Map<Long, String> merchantNames(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Merchant> merchants = dataScopeService.isPlatform()
                ? dataScopeService.withIgnoreTenant(() -> merchantMapper.selectBatchIds(ids))
                : merchantMapper.selectBatchIds(ids);
        return merchants.stream()
                .collect(Collectors.toMap(Merchant::getId, Merchant::getName, (a, b) -> a));
    }

    private Map<Long, String> userNames(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysUser> users = dataScopeService.isPlatform()
                ? dataScopeService.withIgnoreTenant(() -> userMapper.selectBatchIds(ids))
                : userMapper.selectBatchIds(ids);
        return users.stream().collect(Collectors.toMap(SysUser::getId, this::displayName, (a, b) -> a));
    }

    private int resolveStatus(Plan plan) {
        LocalDate today = LocalDate.now();
        if (plan.getStartDate() != null && today.isBefore(plan.getStartDate())) {
            return STATUS_NOT_STARTED;
        }
        if (plan.getEndDate() != null && today.isAfter(plan.getEndDate())) {
            return STATUS_ENDED;
        }
        return STATUS_RUNNING;
    }

    private String userName(Long userId) {
        SysUser user = userId == null ? null : userMapper.selectById(userId);
        return user == null ? null : displayName(user);
    }

    private String displayName(SysUser user) {
        return StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUserName();
    }

    /**
     * 当前登录人，测试可覆盖。
     */
    protected Long currentUserId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * 当前登录人是否租户管理员，测试可覆盖。
     */
    protected boolean currentUserIsTenantAdmin() {
        try {
            if (!StpUtil.isLogin()) {
                return false;
            }
            Object roleKeys = StpUtil.getSession().get("roleKeys");
            return roleKeys instanceof Collection
                    && ((Collection<?>) roleKeys).contains(SysRole.TENANT_ADMIN);
        } catch (Exception ex) {
            log.debug("resolve tenant admin role failed", ex);
            return false;
        }
    }

    private void ensureTenantAdmin() {
        if (dataScopeService.isPlatform()) {
            throw new BizException(403, "平台管理员不能维护租户巡检计划");
        }
        if (!currentUserIsTenantAdmin()) {
            throw new BizException(403, "仅租户管理员可维护巡检计划");
        }
    }
}
