package com.fm.module.danger.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fm.common.context.TenantContext;
import com.fm.common.exception.BizException;
import com.fm.module.danger.dto.DangerAssignDTO;
import com.fm.module.danger.dto.DangerAuditDTO;
import com.fm.module.danger.dto.DangerBatchAssignDTO;
import com.fm.module.danger.dto.DangerDealDTO;
import com.fm.module.danger.dto.DangerQueryDTO;
import com.fm.module.danger.dto.DangerSaveDTO;
import com.fm.module.danger.converter.DangerConverter;
import com.fm.module.danger.entity.Danger;
import com.fm.module.danger.entity.DangerResult;
import com.fm.module.danger.entity.DangerTrace;
import com.fm.module.danger.mapper.DangerMapper;
import com.fm.module.danger.mapper.DangerResultMapper;
import com.fm.module.danger.mapper.DangerTraceMapper;
import com.fm.module.danger.service.DangerService;
import com.fm.module.danger.vo.DangerAssigneeVO;
import com.fm.module.danger.vo.DangerBatchAssignResultVO;
import com.fm.module.danger.vo.DangerTimelineVO;
import com.fm.module.danger.vo.DangerVO;
import com.fm.module.device.entity.Merchant;
import com.fm.module.device.mapper.MerchantMapper;
import com.fm.module.notification.service.MpNotificationService;
import com.fm.module.system.entity.SysRole;
import com.fm.module.system.entity.SysUser;
import com.fm.module.system.entity.SysUserRole;
import com.fm.module.system.entity.SysUserWorkArea;
import com.fm.module.system.entity.SysWorkArea;
import com.fm.module.system.entity.SysWorkAreaMerchant;
import com.fm.module.system.mapper.SysRoleMapper;
import com.fm.module.system.mapper.SysUserMapper;
import com.fm.module.system.mapper.SysUserRoleMapper;
import com.fm.module.system.mapper.SysUserWorkAreaMapper;
import com.fm.module.system.mapper.SysWorkAreaMapper;
import com.fm.module.system.mapper.SysWorkAreaMerchantMapper;
import com.fm.module.system.service.DataScopeService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 隐患闭环实现：来源上报 → 指派 → 整改 → 审核，每一步写时间线并发送站内消息。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DangerServiceImpl implements DangerService {

    private static final long DEFAULT_PAGE_SIZE = 10L;
    private static final long MAX_PAGE_SIZE = 100L;
    private static final int CONTENT_MAX_LENGTH = 500;
    private static final int STATUS_ENABLED = 1;
    private static final Long EMPTY_MERCHANT_ID = -1L;

    private final DangerMapper dangerMapper;
    private final DangerResultMapper resultMapper;
    private final DangerTraceMapper traceMapper;
    private final MerchantMapper merchantMapper;
    private final SysWorkAreaMapper workAreaMapper;
    private final SysWorkAreaMerchantMapper workAreaMerchantMapper;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserWorkAreaMapper userWorkAreaMapper;
    private final DataScopeService dataScopeService;
    private final MpNotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long report(DangerSaveDTO request) {
        return create(request, Danger.SOURCE_SNAPSHOT);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long reportInspection(DangerSaveDTO request) {
        return create(request, Danger.SOURCE_INSPECTION);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assign(Long id, DangerAssignDTO request) {
        AssignOutcome outcome = assignOne(id, request);
        notifyDanger(outcome.getDanger(), Collections.singletonList(outcome.getAssignee()),
                "隐患指派：" + dangerSummary(outcome.getDanger()));
    }

    @Override
    public DangerBatchAssignResultVO batchAssign(DangerBatchAssignDTO request) {
        List<Long> ids = request.getIds().stream().distinct().collect(Collectors.toList());
        if (ids.size() > DangerBatchAssignDTO.MAX_SIZE) {
            throw new BizException("单次最多指派 " + DangerBatchAssignDTO.MAX_SIZE + " 条隐患");
        }
        List<DangerBatchAssignResultVO.Failure> failures = new ArrayList<>();
        List<Danger> assigned = new ArrayList<>();
        DangerAssignDTO assignRequest = new DangerAssignDTO();
        BeanUtils.copyProperties(request, assignRequest);
        SysUser assignee = null;
        for (Long id : ids) {
            try {
                AssignOutcome outcome = transactionTemplate.execute(status -> assignOne(id, assignRequest));
                if (outcome != null) {
                    assigned.add(outcome.getDanger());
                    assignee = outcome.getAssignee();
                }
            } catch (BizException ex) {
                failures.add(new DangerBatchAssignResultVO.Failure(id, ex.getMessage()));
            } catch (Exception ex) {
                log.error("batch assign danger failed, dangerId={}", id, ex);
                failures.add(new DangerBatchAssignResultVO.Failure(id, "处理失败"));
            }
        }
        if (!assigned.isEmpty() && assignee != null) {
            notifyDanger(assigned.get(0), Collections.singletonList(assignee),
                    "隐患批量指派：" + assigned.size() + " 条待整改，请及时处理");
        }
        DangerBatchAssignResultVO result = new DangerBatchAssignResultVO();
        result.setTotal(ids.size());
        result.setSuccessCount(assigned.size());
        result.setFailedCount(failures.size());
        result.setFailures(failures);
        log.info("danger batch assign finished, operator={}, total={}, success={}, failed={}",
                currentUserName(), ids.size(), assigned.size(), failures.size());
        return result;
    }

    /**
     * 单条指派核心逻辑：只做校验与落库留痕，通知由调用方统一发送。
     */
    private AssignOutcome assignOne(Long id, DangerAssignDTO request) {
        Danger danger = requireOperable(id);
        if (danger.getStatus() != Danger.STATUS_PENDING_ASSIGN) {
            throw new BizException("该隐患不是待指派状态");
        }
        Long workAreaId = danger.getWorkAreaId();
        if (workAreaId == null) {
            workAreaId = resolveWorkAreaId(danger.getMerchantId());
            if (workAreaId == null) {
                throw new BizException("商户未绑定工作区，请先在工作区管理中绑定");
            }
            dangerMapper.update(null, new LambdaUpdateWrapper<Danger>()
                    .eq(Danger::getId, id)
                    .set(Danger::getWorkAreaId, workAreaId));
        }
        SysUser assignee = requireFieldUser(request.getAssigneeId(), workAreaId);
        String operator = currentUserName();
        LocalDateTime now = LocalDateTime.now();
        int rows = dangerMapper.update(null, new LambdaUpdateWrapper<Danger>()
                .eq(Danger::getId, id)
                .eq(Danger::getStatus, Danger.STATUS_PENDING_ASSIGN)
                .set(Danger::getStatus, Danger.STATUS_PENDING_DEAL)
                .set(Danger::getAssigneeType, Danger.ASSIGNEE_FIELD)
                .set(Danger::getAssigneeId, assignee.getId())
                .set(Danger::getAssigneeName, displayName(assignee))
                .set(Danger::getAssignBy, operator)
                .set(Danger::getAssignTime, now)
                .set(Danger::getWorkAreaId, workAreaId)
                .set(request.getDealLimit() != null, Danger::getDealLimit, request.getDealLimit())
                .set(request.getDangerLevelFinal() != null, Danger::getDangerLevelFinal,
                        request.getDangerLevelFinal())
                .set(Danger::getUpdateTime, now));
        if (rows == 0) {
            throw new BizException("该隐患已被指派");
        }
        Map<String, Object> detail = new HashMap<>(4);
        detail.put("assigneeId", assignee.getId());
        detail.put("assigneeName", displayName(assignee));
        detail.put("dealLimit", request.getDealLimit());
        detail.put("remark", request.getRemark());
        addTrace(danger, DangerTrace.TYPE_ASSIGN, 1,
                "指派给 " + displayName(assignee) + limitText(request.getDealLimit()), detail);
        danger.setAssigneeType(Danger.ASSIGNEE_FIELD);
        danger.setAssigneeId(assignee.getId());
        danger.setAssigneeName(displayName(assignee));
        danger.setDealLimit(request.getDealLimit());
        return new AssignOutcome(danger, assignee);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deal(Long id, DangerDealDTO request) {
        Danger danger = requireOperable(id);
        if (danger.getStatus() != Danger.STATUS_PENDING_DEAL) {
            throw new BizException("只有待整改的隐患才能提交整改");
        }
        ensureHandler(danger);
        Long operatorId = currentUserId();
        String operator = currentUserName();
        LocalDateTime now = LocalDateTime.now();
        int rows = dangerMapper.update(null, new LambdaUpdateWrapper<Danger>()
                .eq(Danger::getId, id)
                .eq(Danger::getStatus, Danger.STATUS_PENDING_DEAL)
                .set(Danger::getStatus, Danger.STATUS_PENDING_AUDIT)
                .set(Danger::getDealBy, operator)
                .set(Danger::getDealTime, now)
                .set(Danger::getAuditBy, null)
                .set(Danger::getAuditTime, null)
                .set(Danger::getAuditResult, null)
                .set(Danger::getAuditRemark, null)
                .set(Danger::getUpdateTime, now));
        if (rows == 0) {
            throw new BizException("该隐患已提交整改");
        }
        DangerResult result = new DangerResult();
        result.setTenantId(danger.getTenantId());
        result.setDangerId(danger.getId());
        result.setDealUserId(operatorId);
        result.setDealUserName(operator);
        result.setDealBy(operator);
        result.setDealTime(now);
        result.setRemark(request.getRemark());
        result.setPic1(request.getPic1());
        result.setPic2(request.getPic2());
        result.setPic3(request.getPic3());
        result.setCreateTime(now);
        result.setUpdateTime(now);
        resultMapper.insert(result);
        addTrace(danger, DangerTrace.TYPE_DEAL, 1, "整改提交：" + request.getRemark(),
                pics(request.getPic1(), request.getPic2(), request.getPic3()));
        danger.setStatus(Danger.STATUS_PENDING_AUDIT);
        danger.setDealTime(now);
        notifyDanger(danger, tenantAdmins(danger.getTenantId()), "隐患待审核：" + dangerSummary(danger));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long id, DangerAuditDTO request) {
        ensureDangerOperator();
        Danger danger = requireOperable(id);
        if (danger.getStatus() != Danger.STATUS_PENDING_AUDIT) {
            throw new BizException("只有待审核的隐患才能审核");
        }
        Integer auditResult = request.getAuditResult();
        boolean passed = Integer.valueOf(DangerAuditDTO.RESULT_PASS).equals(auditResult);
        boolean rejected = Integer.valueOf(DangerAuditDTO.RESULT_REJECT).equals(auditResult);
        if (!passed && !rejected) {
            throw new BizException("审核结论不合法");
        }
        if (rejected && !StringUtils.hasText(request.getAuditRemark())) {
            throw new BizException("驳回必须填写原因");
        }
        Long operatorId = currentUserId();
        String operator = currentUserName();
        LocalDateTime now = LocalDateTime.now();
        int rows = dangerMapper.update(null, new LambdaUpdateWrapper<Danger>()
                .eq(Danger::getId, id)
                .eq(Danger::getStatus, Danger.STATUS_PENDING_AUDIT)
                .set(Danger::getStatus, passed ? Danger.STATUS_DONE : Danger.STATUS_PENDING_DEAL)
                .set(Danger::getAuditBy, operatorId)
                .set(Danger::getAuditByName, operator)
                .set(Danger::getAuditTime, now)
                .set(Danger::getAuditResult, auditResult)
                .set(Danger::getAuditRemark, request.getAuditRemark())
                .set(Danger::getUpdateTime, now));
        if (rows == 0) {
            throw new BizException("该隐患已审核");
        }
        updateLatestResultAudit(danger.getId(), operatorId, operator, request, now);
        Map<String, Object> detail = new HashMap<>(4);
        detail.put("auditResult", auditResult);
        detail.put("auditRemark", request.getAuditRemark());
        addTrace(danger, DangerTrace.TYPE_AUDIT, passed ? 1 : 2,
                passed ? "审核通过：" + safe(request.getAuditRemark())
                        : "审核驳回：" + request.getAuditRemark(),
                detail);
        danger.setStatus(passed ? Danger.STATUS_DONE : Danger.STATUS_PENDING_DEAL);
        danger.setAuditResult(auditResult);
        danger.setAuditRemark(request.getAuditRemark());
        notifyDanger(danger, handlerUsers(danger),
                passed ? "隐患审核通过：" + dangerSummary(danger)
                        : "隐患被驳回：" + safe(request.getAuditRemark()));
    }

    @Override
    public Page<DangerVO> page(DangerQueryDTO query) {
        long pageNum = query.getPageNum() < 1 ? 1 : query.getPageNum();
        long pageSize = query.getPageSize() < 1
                ? DEFAULT_PAGE_SIZE : Math.min(query.getPageSize(), MAX_PAGE_SIZE);
        LambdaQueryWrapper<Danger> wrapper = buildQueryWrapper(query);
        applyScope(wrapper);
        wrapper.orderByDesc(Danger::getCreateTime).orderByDesc(Danger::getId);
        Page<Danger> page;
        if (dataScopeService.isPlatform()) {
            List<Long> tenantIds = dataScopeService.authorizedTenantIds();
            if (tenantIds.isEmpty()) {
                return new Page<>(pageNum, pageSize);
            }
            wrapper.in(Danger::getTenantId, tenantIds);
            page = dataScopeService.withIgnoreTenant(() -> dangerMapper.selectPage(
                    new Page<>(pageNum, pageSize), wrapper));
        } else {
            page = dangerMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        }
        Page<DangerVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(toVOList(page.getRecords()));
        return result;
    }

    @Override
    public DangerVO detail(Long id) {
        Danger danger = requireVisible(id);
        return toVOList(Collections.singletonList(danger)).get(0);
    }

    @Override
    public List<DangerTimelineVO> timeline(Long id) {
        Danger danger = requireVisible(id);
        List<DangerTrace> traces = listTraces(id);
        DangerResult lastDeal = latestDeal(id);
        List<DangerTimelineVO> nodes = new ArrayList<>(6);
        nodes.add(reportNode(danger));
        nodes.add(alarmNode(danger));
        nodes.add(convertNode(danger, traces));
        nodes.add(assignNode(danger));
        nodes.add(dealNode(lastDeal));
        nodes.add(auditNode(danger, lastDeal));
        return nodes;
    }

    @Override
    public List<DangerAssigneeVO> assigneeOptions(Long dangerId) {
        Danger danger = requireVisible(dangerId);
        Long workAreaId = danger.getWorkAreaId();
        if (workAreaId == null) {
            return Collections.emptyList();
        }
        List<Long> userIds = userWorkAreaMapper.selectList(new LambdaQueryWrapper<SysUserWorkArea>()
                        .eq(SysUserWorkArea::getWorkAreaId, workAreaId))
                .stream().map(SysUserWorkArea::getUserId).distinct().collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> fieldUserIds = fieldUserIds(userIds);
        if (fieldUserIds.isEmpty()) {
            return Collections.emptyList();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .filter(user -> fieldUserIds.contains(user.getId()))
                .filter(user -> Integer.valueOf(STATUS_ENABLED).equals(user.getStatus()))
                .map(user -> DangerConverter.toAssigneeVO(user, displayName(user)))
                .collect(Collectors.toList());
    }

    private Long create(DangerSaveDTO request, int sourceType) {
        Merchant merchant = merchantMapper.selectById(request.getMerchantId());
        if (merchant == null) {
            throw new BizException("商户不存在");
        }
        if (SysRole.SCOPE_WORK_AREA.equals(TenantContext.getDataScope())) {
            List<Long> allowedMerchantIds = dataScopeService.workAreaMerchantIds(currentUserId());
            if (!allowedMerchantIds.contains(merchant.getId())) {
                throw new BizException(403, "只能上报所属工作区的商户");
            }
        }
        LocalDateTime now = LocalDateTime.now();
        Long operatorId = currentUserId();
        String operator = currentUserName();
        Danger danger = new Danger();
        danger.setTenantId(merchant.getTenantId());
        danger.setMerchantId(merchant.getId());
        danger.setSourceType(sourceType);
        danger.setDangerType(request.getDangerType());
        danger.setRemark(request.getRemark());
        danger.setPosition(StringUtils.hasText(request.getPosition())
                ? request.getPosition() : merchant.getAddr());
        danger.setPositionDetail(request.getPositionDetail());
        danger.setLng(request.getLng());
        danger.setLat(request.getLat());
        danger.setPic1(request.getPic1());
        danger.setPic2(request.getPic2());
        danger.setPic3(request.getPic3());
        danger.setDangerLevel(request.getDangerLevel());
        danger.setDealLimit(request.getDealLimit());
        danger.setStatus(Danger.STATUS_PENDING_DEAL);
        danger.setAssigneeType(Danger.ASSIGNEE_MERCHANT);
        danger.setAssigneeId(resolveMerchantUserId(merchant.getId()));
        danger.setAssigneeName(merchant.getName());
        danger.setWorkAreaId(resolveWorkAreaId(merchant.getId()));
        danger.setCreateBy(operator);
        danger.setAssignBy(operator);
        danger.setAssignTime(now);
        danger.setCreateTime(now);
        danger.setUpdateTime(now);
        dangerMapper.insert(danger);
        Map<String, Object> detail = new HashMap<>(4);
        detail.put("sourceType", sourceType);
        detail.put("merchantId", merchant.getId());
        detail.put("operatorId", operatorId);
        addTrace(danger, DangerTrace.TYPE_REPORT, 1,
                "上报隐患：" + dangerSummary(danger),
                pics(danger.getPic1(), danger.getPic2(), danger.getPic3()));
        notifyDanger(danger, merchantUsers(merchant.getId()), "隐患待整改：" + dangerSummary(danger));
        log.info("danger reported, id={}, sourceType={}, merchantId={}",
                danger.getId(), sourceType, merchant.getId());
        return danger.getId();
    }

    private LambdaQueryWrapper<Danger> buildQueryWrapper(DangerQueryDTO query) {
        LambdaQueryWrapper<Danger> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(query.getStatus() != null, Danger::getStatus, query.getStatus())
                .in(Boolean.TRUE.equals(query.getUnfinished()), Danger::getStatus,
                        Danger.STATUS_PENDING_ASSIGN, Danger.STATUS_PENDING_DEAL,
                        Danger.STATUS_PENDING_AUDIT, Danger.STATUS_REJECTED)
                .eq(query.getSourceType() != null, Danger::getSourceType, query.getSourceType())
                .eq(query.getMerchantId() != null, Danger::getMerchantId, query.getMerchantId())
                .eq(StringUtils.hasText(query.getDangerType()), Danger::getDangerType, query.getDangerType())
                .ge(query.getDealLimitFrom() != null, Danger::getDealLimit, query.getDealLimitFrom())
                .le(query.getDealLimitTo() != null, Danger::getDealLimit, query.getDealLimitTo());
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.likeRight(Danger::getRemark, query.getKeyword())
                    .or().likeRight(Danger::getPosition, query.getKeyword()));
        }
        return wrapper;
    }

    private void applyScope(LambdaQueryWrapper<Danger> wrapper) {
        if (dataScopeService.isPlatform()) {
            return;
        }
        if (MpNotificationService.USER_TYPE_MERCHANT.equals(TenantContext.getUserType())) {
            Long merchantId = TenantContext.getMerchantId();
            wrapper.eq(Danger::getMerchantId, merchantId == null ? EMPTY_MERCHANT_ID : merchantId);
            return;
        }
        if (SysRole.SCOPE_WORK_AREA.equals(TenantContext.getDataScope())) {
            wrapper.eq(Danger::getAssigneeType, Danger.ASSIGNEE_FIELD)
                    .eq(Danger::getAssigneeId, currentUserId());
        }
    }

    private Danger requireOperable(Long id) {
        if (dataScopeService.isPlatform()) {
            throw new BizException(403, "平台管理员不能操作租户隐患");
        }
        return requireVisible(id);
    }

    private Danger requireVisible(Long id) {
        if (id == null) {
            throw new BizException("隐患不存在");
        }
        Danger danger = dataScopeService.isPlatform()
                ? dataScopeService.withIgnoreTenant(() -> dangerMapper.selectById(id))
                : dangerMapper.selectById(id);
        if (danger == null) {
            throw new BizException("隐患不存在");
        }
        if (dataScopeService.isPlatform()) {
            if (!dataScopeService.canAccess(danger.getTenantId())) {
                throw new BizException("隐患不存在");
            }
            return danger;
        }
        if (MpNotificationService.USER_TYPE_MERCHANT.equals(TenantContext.getUserType())) {
            if (!Objects.equals(danger.getMerchantId(), TenantContext.getMerchantId())) {
                throw new BizException("隐患不存在");
            }
            return danger;
        }
        if (SysRole.SCOPE_WORK_AREA.equals(TenantContext.getDataScope())) {
            if (!Danger.ASSIGNEE_FIELD.equals(danger.getAssigneeType())
                    || !Objects.equals(danger.getAssigneeId(), currentUserId())) {
                throw new BizException("隐患不存在");
            }
        }
        return danger;
    }

    private void ensureHandler(Danger danger) {
        if (MpNotificationService.USER_TYPE_MERCHANT.equals(TenantContext.getUserType())) {
            if (!Danger.ASSIGNEE_MERCHANT.equals(danger.getAssigneeType())
                    || !Objects.equals(danger.getMerchantId(), TenantContext.getMerchantId())) {
                throw new BizException(403, "只能整改本商户的隐患");
            }
            return;
        }
        if (!Danger.ASSIGNEE_FIELD.equals(danger.getAssigneeType())
                || !Objects.equals(danger.getAssigneeId(), currentUserId())) {
            throw new BizException(403, "只能整改指派给自己的隐患");
        }
    }

    private void ensureDangerOperator() {
        if (dataScopeService.isPlatform()) {
            throw new BizException(403, "平台管理员不能审核租户隐患");
        }
        if (!currentUserIsTenantAdmin() && !currentUserHasRole(SysRole.TENANT_DUTY)) {
            throw new BizException(403, "仅租户管理员或值守可审核隐患");
        }
    }

    private SysUser requireFieldUser(Long userId, Long workAreaId) {
        SysUser user = userId == null ? null : userMapper.selectById(userId);
        if (user == null || !Integer.valueOf(STATUS_ENABLED).equals(user.getStatus())) {
            throw new BizException("外勤不存在或已停用");
        }
        if (!TenantContext.getTenantId().equals(user.getTenantId())) {
            throw new BizException("外勤不属于当前运营中心");
        }
        if (!isFieldUser(userId)) {
            throw new BizException("所选人员不是外勤");
        }
        Long bound = userWorkAreaMapper.selectCount(new LambdaQueryWrapper<SysUserWorkArea>()
                .eq(SysUserWorkArea::getUserId, userId)
                .eq(SysUserWorkArea::getWorkAreaId, workAreaId));
        if (bound == null || bound == 0) {
            throw new BizException("所选人员不属于该隐患工作区");
        }
        return user;
    }

    private boolean isFieldUser(Long userId) {
        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleKey, SysRole.TENANT_FIELD)
                .last("limit 1"));
        if (role == null) {
            return false;
        }
        Long count = userRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getRoleId, role.getId())
                .eq(SysUserRole::getUserId, userId));
        return count != null && count > 0;
    }

    private Set<Long> fieldUserIds(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptySet();
        }
        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleKey, SysRole.TENANT_FIELD)
                .last("limit 1"));
        if (role == null) {
            return Collections.emptySet();
        }
        return userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getRoleId, role.getId())
                        .in(SysUserRole::getUserId, userIds))
                .stream().map(SysUserRole::getUserId).collect(Collectors.toCollection(HashSet::new));
    }

    private Long resolveMerchantUserId(Long merchantId) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getMerchantId, merchantId)
                .eq(SysUser::getUserType, MpNotificationService.USER_TYPE_MERCHANT)
                .eq(SysUser::getStatus, STATUS_ENABLED)
                .orderByAsc(SysUser::getId)
                .last("limit 1"));
        return user == null ? null : user.getId();
    }

    private Long resolveWorkAreaId(Long merchantId) {
        if (merchantId == null) {
            return null;
        }
        SysWorkAreaMerchant relation = workAreaMerchantMapper.selectOne(
                new LambdaQueryWrapper<SysWorkAreaMerchant>()
                        .eq(SysWorkAreaMerchant::getMerchantId, merchantId)
                        .orderByAsc(SysWorkAreaMerchant::getId)
                        .last("limit 1"));
        return relation == null ? null : relation.getWorkAreaId();
    }

    private List<SysUser> merchantUsers(Long merchantId) {
        if (merchantId == null) {
            return Collections.emptyList();
        }
        return userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getMerchantId, merchantId)
                .eq(SysUser::getUserType, MpNotificationService.USER_TYPE_MERCHANT)
                .eq(SysUser::getStatus, STATUS_ENABLED));
    }

    private List<SysUser> tenantAdmins(Long tenantId) {
        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleKey, SysRole.TENANT_ADMIN)
                .eq(SysRole::getTenantId, tenantId)
                .last("limit 1"));
        if (role == null) {
            return Collections.emptyList();
        }
        List<Long> userIds = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getRoleId, role.getId()))
                .stream().map(SysUserRole::getUserId).collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return Collections.emptyList();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .filter(user -> Integer.valueOf(STATUS_ENABLED).equals(user.getStatus()))
                .collect(Collectors.toList());
    }

    private List<SysUser> handlerUsers(Danger danger) {
        if (Danger.ASSIGNEE_FIELD.equals(danger.getAssigneeType())) {
            SysUser user = danger.getAssigneeId() == null
                    ? null : userMapper.selectById(danger.getAssigneeId());
            return user == null ? Collections.emptyList() : Collections.singletonList(user);
        }
        return merchantUsers(danger.getMerchantId());
    }

    private void notifyDanger(Danger danger, Collection<SysUser> receivers, String content) {
        try {
            notificationService.sendDanger(danger, receivers, content);
        } catch (Exception ex) {
            log.warn("send danger notification failed, dangerId={}", danger.getId(), ex);
        }
    }

    private List<DangerVO> toVOList(List<Danger> list) {
        if (list.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> merchantIds = list.stream().map(Danger::getMerchantId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> workAreaIds = list.stream().map(Danger::getWorkAreaId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> dangerIds = list.stream().map(Danger::getId).collect(Collectors.toSet());
        Map<Long, String> merchantNames = merchantNames(merchantIds);
        Map<Long, String> workAreaNames = workAreaNames(workAreaIds);
        Map<Long, DangerResult> latestDeals = latestDealMap(dangerIds);
        return list.stream().map(item -> DangerConverter.toVO(item,
                        merchantNames.get(item.getMerchantId()), workAreaNames.get(item.getWorkAreaId()),
                        latestDeals.get(item.getId())))
                .collect(Collectors.toList());
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

    private Map<Long, DangerResult> latestDealMap(Collection<Long> dangerIds) {
        if (dangerIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<DangerResult> wrapper = new LambdaQueryWrapper<DangerResult>()
                .in(DangerResult::getDangerId, dangerIds)
                .orderByDesc(DangerResult::getId);
        List<DangerResult> results = dataScopeService.isPlatform()
                ? dataScopeService.withIgnoreTenant(() -> resultMapper.selectList(wrapper))
                : resultMapper.selectList(wrapper);
        Map<Long, DangerResult> latest = new HashMap<>(results.size());
        results.forEach(item -> latest.putIfAbsent(item.getDangerId(), item));
        return latest;
    }

    private DangerResult latestDeal(Long dangerId) {
        LambdaQueryWrapper<DangerResult> wrapper = new LambdaQueryWrapper<DangerResult>()
                .eq(DangerResult::getDangerId, dangerId)
                .orderByDesc(DangerResult::getId)
                .last("limit 1");
        return dataScopeService.isPlatform()
                ? dataScopeService.withIgnoreTenant(() -> resultMapper.selectOne(wrapper))
                : resultMapper.selectOne(wrapper);
    }

    private List<DangerTrace> listTraces(Long dangerId) {
        LambdaQueryWrapper<DangerTrace> wrapper = new LambdaQueryWrapper<DangerTrace>()
                .eq(DangerTrace::getDangerId, dangerId)
                .orderByAsc(DangerTrace::getCreateTime)
                .orderByAsc(DangerTrace::getId);
        return dataScopeService.isPlatform()
                ? dataScopeService.withIgnoreTenant(() -> traceMapper.selectList(wrapper))
                : traceMapper.selectList(wrapper);
    }

    private void updateLatestResultAudit(Long dangerId, Long operatorId, String operator,
                                         DangerAuditDTO request, LocalDateTime now) {
        DangerResult lastDeal = latestDeal(dangerId);
        if (lastDeal == null) {
            return;
        }
        resultMapper.update(null, new LambdaUpdateWrapper<DangerResult>()
                .eq(DangerResult::getId, lastDeal.getId())
                .set(DangerResult::getAuditUserId, operatorId)
                .set(DangerResult::getAuditUserName, operator)
                .set(DangerResult::getAuditResult, request.getAuditResult())
                .set(DangerResult::getAuditRemark, request.getAuditRemark())
                .set(DangerResult::getAuditPic1, request.getAuditPic1())
                .set(DangerResult::getAuditPic2, request.getAuditPic2())
                .set(DangerResult::getAuditPic3, request.getAuditPic3())
                .set(DangerResult::getUpdateTime, now));
    }

    private DangerTimelineVO reportNode(Danger danger) {
        DangerTimelineVO node = new DangerTimelineVO();
        node.setNodeKey(DangerTimelineVO.NODE_REPORT);
        node.setTitle("上报");
        node.setOccurred(danger.getCreateTime() != null);
        node.setTime(danger.getCreateTime());
        node.setOperator(danger.getCreateBy());
        node.setResult(1);
        node.setContent(StringUtils.hasText(danger.getRemark()) ? danger.getRemark() : "隐患上报");
        node.setPics(pics(danger.getPic1(), danger.getPic2(), danger.getPic3()));
        return node;
    }

    private DangerTimelineVO alarmNode(Danger danger) {
        DangerTimelineVO node = new DangerTimelineVO();
        node.setNodeKey(DangerTimelineVO.NODE_ALARM);
        node.setTitle("报警");
        node.setOccurred(danger.getAlarmTime() != null);
        node.setTime(danger.getAlarmTime());
        node.setResult(1);
        node.setContent(danger.getAlarmTime() == null ? null : "设备报警上报");
        return node;
    }

    private DangerTimelineVO convertNode(Danger danger, List<DangerTrace> traces) {
        DangerTimelineVO node = new DangerTimelineVO();
        node.setNodeKey(DangerTimelineVO.NODE_CONVERT);
        node.setTitle("转隐患");
        node.setOccurred(danger.getConvertTime() != null);
        node.setTime(danger.getConvertTime());
        node.setResult(1);
        String convertSource = convertSource(traces);
        node.setExtra(convertSource);
        node.setContent(danger.getConvertTime() == null ? null
                : "由故障报警转隐患（" + convertSourceText(convertSource) + "）");
        return node;
    }

    private DangerTimelineVO assignNode(Danger danger) {
        DangerTimelineVO node = new DangerTimelineVO();
        node.setNodeKey(DangerTimelineVO.NODE_ASSIGN);
        node.setTitle("指派");
        node.setOccurred(danger.getAssignTime() != null);
        node.setTime(danger.getAssignTime());
        node.setOperator(danger.getAssignBy());
        node.setResult(1);
        if (danger.getAssignTime() != null) {
            node.setContent("指派给 " + safe(danger.getAssigneeName()) + limitText(danger.getDealLimit()));
        }
        return node;
    }

    private DangerTimelineVO dealNode(DangerResult lastDeal) {
        DangerTimelineVO node = new DangerTimelineVO();
        node.setNodeKey(DangerTimelineVO.NODE_DEAL);
        node.setTitle("整改");
        node.setOccurred(lastDeal != null && lastDeal.getDealTime() != null);
        if (lastDeal != null) {
            node.setTime(lastDeal.getDealTime());
            node.setOperator(StringUtils.hasText(lastDeal.getDealUserName())
                    ? lastDeal.getDealUserName() : lastDeal.getDealBy());
            node.setContent(lastDeal.getRemark());
            node.setPics(pics(lastDeal.getPic1(), lastDeal.getPic2(), lastDeal.getPic3()));
            node.setResult(1);
        }
        return node;
    }

    private DangerTimelineVO auditNode(Danger danger, DangerResult lastDeal) {
        DangerTimelineVO node = new DangerTimelineVO();
        node.setNodeKey(DangerTimelineVO.NODE_AUDIT);
        node.setTitle("审核");
        node.setOccurred(danger.getAuditTime() != null);
        node.setTime(danger.getAuditTime());
        String operator = StringUtils.hasText(danger.getAuditByName())
                ? danger.getAuditByName() : (lastDeal == null ? null : lastDeal.getAuditUserName());
        node.setOperator(operator);
        node.setResult(danger.getAuditResult() == null || danger.getAuditResult() == 1 ? 1 : 2);
        if (danger.getAuditTime() != null) {
            boolean passed = !Integer.valueOf(DangerAuditDTO.RESULT_REJECT).equals(danger.getAuditResult());
            node.setContent((passed ? "审核通过" : "审核驳回")
                    + (StringUtils.hasText(danger.getAuditRemark()) ? "：" + danger.getAuditRemark() : ""));
        }
        return node;
    }

    private String convertSource(List<DangerTrace> traces) {
        for (DangerTrace trace : traces) {
            if (trace.getTraceType() == null || trace.getTraceType() != DangerTrace.TYPE_CONVERT) {
                continue;
            }
            if (!StringUtils.hasText(trace.getDetail())) {
                continue;
            }
            try {
                JsonNode detail = objectMapper.readTree(trace.getDetail());
                String source = detail.path("convertSource").asText();
                if (StringUtils.hasText(source)) {
                    return source;
                }
            } catch (Exception ex) {
                log.debug("parse danger trace detail failed, traceId={}", trace.getId(), ex);
            }
        }
        return null;
    }

    private String convertSourceText(String convertSource) {
        if ("AUTO".equals(convertSource)) {
            return "自动转";
        }
        if ("MANUAL".equals(convertSource)) {
            return "人工转";
        }
        return "未知来源";
    }

    private void addTrace(Danger danger, int traceType, Integer result, String content, Object detail) {
        DangerTrace trace = new DangerTrace();
        trace.setTenantId(danger.getTenantId());
        trace.setDangerId(danger.getId());
        trace.setTraceType(traceType);
        trace.setResult(result);
        trace.setContent(truncate(content));
        trace.setDetail(toJson(detail));
        trace.setCreateTime(LocalDateTime.now());
        traceMapper.insert(trace);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            log.warn("serialize danger detail failed", ex);
            return null;
        }
    }

    private String dangerSummary(Danger danger) {
        return safe(danger.getDangerType()) + " " + safe(danger.getPosition());
    }

    private String limitText(LocalDate dealLimit) {
        return dealLimit == null ? "" : "，整改期限 " + dealLimit;
    }

    private List<String> pics(String pic1, String pic2, String pic3) {
        List<String> pics = new ArrayList<>(3);
        if (StringUtils.hasText(pic1)) {
            pics.add(pic1);
        }
        if (StringUtils.hasText(pic2)) {
            pics.add(pic2);
        }
        if (StringUtils.hasText(pic3)) {
            pics.add(pic3);
        }
        return pics;
    }

    private String displayName(SysUser user) {
        return StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUserName();
    }

    private String truncate(String content) {
        if (content == null) {
            return null;
        }
        return content.length() <= CONTENT_MAX_LENGTH ? content : content.substring(0, CONTENT_MAX_LENGTH);
    }

    private String safe(String value) {
        return StringUtils.hasText(value) ? value : "-";
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
     * 当前登录人姓名，测试可覆盖。
     */
    protected String currentUserName() {
        Long userId = currentUserId();
        SysUser user = userId == null ? null : userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        return displayName(user);
    }

    /**
     * 当前登录人是否租户管理员，测试可覆盖。
     */
    protected boolean currentUserIsTenantAdmin() {
        return currentUserHasRole(SysRole.TENANT_ADMIN);
    }

    protected boolean currentUserHasRole(String roleKey) {
        try {
            if (!StpUtil.isLogin()) {
                return false;
            }
            Object roleKeys = StpUtil.getSession().get("roleKeys");
            return roleKeys instanceof Collection
                    && ((Collection<?>) roleKeys).contains(roleKey);
        } catch (Exception ex) {
            log.debug("resolve user role failed, roleKey={}", roleKey, ex);
            return false;
        }
    }

    /**
     * 单条指派结果：供单条通知与批量汇总通知复用。
     */
    @Getter
    @RequiredArgsConstructor
    private static class AssignOutcome {

        private final Danger danger;
        private final SysUser assignee;
    }
}
