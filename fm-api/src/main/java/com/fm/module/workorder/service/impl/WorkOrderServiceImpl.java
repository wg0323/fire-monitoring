package com.fm.module.workorder.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fm.common.context.TenantContext;
import com.fm.common.exception.BizException;
import com.fm.module.alarm.entity.FireAlarm;
import com.fm.module.alarm.mapper.FireAlarmMapper;
import com.fm.module.alarm.service.AlarmDomainService;
import com.fm.module.device.entity.Merchant;
import com.fm.module.device.mapper.MerchantMapper;
import com.fm.module.system.entity.SysRole;
import com.fm.module.system.entity.SysUser;
import com.fm.module.system.entity.SysWorkArea;
import com.fm.module.system.mapper.SysUserMapper;
import com.fm.module.system.mapper.SysWorkAreaMapper;
import com.fm.module.system.service.DataScopeService;
import com.fm.module.workorder.converter.WorkOrderConverter;
import com.fm.module.workorder.dto.WorkOrderFinishDTO;
import com.fm.module.workorder.dto.WorkOrderQueryDTO;
import com.fm.module.workorder.entity.WorkOrder;
import com.fm.module.workorder.mapper.WorkOrderMapper;
import com.fm.module.workorder.service.WorkOrderService;
import com.fm.module.workorder.vo.WorkOrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 工单实现：0 待接单 → 1 处理中 → 2 已完成；完成时同事务收口报警。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderServiceImpl extends ServiceImpl<WorkOrderMapper, WorkOrder> implements WorkOrderService {

    private static final String USER_TYPE_MERCHANT = "MERCHANT";
    private static final String DANGER_PENDING = "PENDING";
    private static final String DANGER_CONVERTED = "CONVERTED";
    private static final String DANGER_RECOVERED = "RECOVERED";
    private static final String DANGER_NONE = "NONE";

    private final SysUserMapper userMapper;
    private final SysWorkAreaMapper workAreaMapper;
    private final MerchantMapper merchantMapper;
    private final FireAlarmMapper alarmMapper;
    private final AlarmDomainService alarmDomainService;
    private final DataScopeService dataScopeService;

    @Override
    public Page<WorkOrderVO> pageVO(WorkOrderQueryDTO query) {
        return doPage(query, false);
    }

    @Override
    public Page<WorkOrderVO> mpPage(WorkOrderQueryDTO query) {
        return doPage(query, true);
    }

    @Override
    public long countMpTodo() {
        if (USER_TYPE_MERCHANT.equals(TenantContext.getUserType())) {
            return 0L;
        }
        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<WorkOrder>()
                .in(WorkOrder::getStatus, WorkOrder.STATUS_PENDING, WorkOrder.STATUS_PROCESSING);
        if (SysRole.SCOPE_WORK_AREA.equals(TenantContext.getDataScope())) {
            wrapper.eq(WorkOrder::getHandlerId, currentUserId());
        }
        Long count;
        if (dataScopeService.isPlatform()) {
            List<Long> tenantIds = dataScopeService.authorizedTenantIds();
            if (tenantIds.isEmpty()) {
                return 0L;
            }
            wrapper.in(WorkOrder::getTenantId, tenantIds);
            count = dataScopeService.withIgnoreTenant(() -> baseMapper.selectCount(wrapper));
        } else {
            count = baseMapper.selectCount(wrapper);
        }
        return count == null ? 0L : count;
    }

    private Page<WorkOrderVO> doPage(WorkOrderQueryDTO query, boolean miniapp) {
        long pageNum = query.getPageNum() < 1 ? 1 : query.getPageNum();
        long pageSize = query.getPageSize() < 1 ? 10 : Math.min(query.getPageSize(), 100);
        if (miniapp && USER_TYPE_MERCHANT.equals(TenantContext.getUserType())) {
            return new Page<>(pageNum, pageSize, 0);
        }
        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(query.getStatus() != null, WorkOrder::getStatus, query.getStatus());
        wrapper.eq(query.getMerchantId() != null, WorkOrder::getMerchantId, query.getMerchantId());
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(WorkOrder::getTitle, query.getKeyword())
                    .or().like(WorkOrder::getContent, query.getKeyword()));
        }
        wrapper.orderByAsc(WorkOrder::getStatus).orderByDesc(WorkOrder::getCreateTime);
        Page<WorkOrder> page = new Page<>(pageNum, pageSize);
        if (miniapp && SysRole.SCOPE_WORK_AREA.equals(TenantContext.getDataScope())) {
            wrapper.eq(WorkOrder::getHandlerId, currentUserId());
        }
        Page<WorkOrder> result;
        if (dataScopeService.isPlatform()) {
            List<Long> tenantIds = dataScopeService.authorizedTenantIds();
            if (tenantIds.isEmpty()) {
                return new Page<>(pageNum, pageSize, 0);
            }
            wrapper.in(WorkOrder::getTenantId, tenantIds);
            result = dataScopeService.withIgnoreTenant(() -> baseMapper.selectPage(page, wrapper));
        } else {
            result = baseMapper.selectPage(page, wrapper);
        }
        Page<WorkOrderVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<WorkOrderVO> records = result.getRecords().stream()
                .map(WorkOrderConverter::toVO).collect(Collectors.toList());
        fillExtra(records);
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public WorkOrderVO detailVO(Long id) {
        WorkOrderVO vo = WorkOrderConverter.toVO(require(id));
        fillExtra(Collections.singletonList(vo));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createFromAlarm(Long tenantId, Long merchantId, Long alarmId, Long workAreaId,
                                String title, String content, Integer priority, Long handlerId, String handlerName) {
        Long exists = baseMapper.selectCount(new LambdaQueryWrapper<WorkOrder>()
                .eq(WorkOrder::getAlarmId, alarmId));
        if (exists != null && exists > 0) {
            return;
        }
        WorkOrder order = new WorkOrder();
        order.setTenantId(tenantId);
        order.setMerchantId(merchantId);
        order.setAlarmId(alarmId);
        order.setWorkAreaId(workAreaId);
        order.setSource(WorkOrder.SOURCE_DISPATCH);
        order.setTitle(truncate(title, 128));
        order.setContent(truncate(content, 500));
        order.setHandlerId(handlerId);
        order.setHandlerName(truncate(handlerName, 64));
        order.setStatus(WorkOrder.STATUS_PENDING);
        order.setPriority(priority == null ? 1 : priority);
        try {
            baseMapper.insert(order);
        } catch (DuplicateKeyException ex) {
            log.info("duplicate work order ignored, alarmId={}", alarmId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void accept(Long id) {
        WorkOrder order = require(id);
        if (!Integer.valueOf(WorkOrder.STATUS_PENDING).equals(order.getStatus())) {
            throw new BizException("只有待接单工单可以接单");
        }
        Long userId = currentUserId();
        if (order.getHandlerId() != null && !order.getHandlerId().equals(userId)) {
            throw new BizException(403, "该工单已指派给其他处理人");
        }
        int rows = baseMapper.update(null, new LambdaUpdateWrapper<WorkOrder>()
                .eq(WorkOrder::getId, id)
                .eq(WorkOrder::getStatus, WorkOrder.STATUS_PENDING)
                .set(WorkOrder::getStatus, WorkOrder.STATUS_PROCESSING)
                .set(WorkOrder::getHandlerId, userId)
                .set(WorkOrder::getHandlerName, currentUserName(userId))
                .set(WorkOrder::getAcceptTime, LocalDateTime.now())
                .set(WorkOrder::getUpdateTime, LocalDateTime.now()));
        if (rows == 0) {
            throw new BizException("工单已被接单");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finish(WorkOrderFinishDTO request) {
        if (!Integer.valueOf(FireAlarm.RESULT_FALSE).equals(request.getResult())
                && !Integer.valueOf(FireAlarm.RESULT_TRUE).equals(request.getResult())) {
            throw new BizException("处理结论只能是误报或真实火警");
        }
        WorkOrder order = require(request.getId());
        if (!Integer.valueOf(WorkOrder.STATUS_PROCESSING).equals(order.getStatus())) {
            throw new BizException("只有处理中的工单可以完成");
        }
        Long userId = currentUserId();
        if (!Objects.equals(order.getHandlerId(), userId)) {
            throw new BizException(403, "只有工单处理人可以完成");
        }
        FireAlarm alarm = order.getAlarmId() == null ? null : alarmMapper.selectById(order.getAlarmId());
        if (alarm == null) {
            throw new BizException("来源报警不存在");
        }
        if (AlarmDomainService.isFault(alarm)) {
            // 故障报警由转隐患或自行恢复链路收口，完工不能清空转隐患截止时间
            log.info("fault alarm closed by danger flow, skip finalizeAlarm, alarmId={}", alarm.getId());
        } else {
            String userName = currentUserName(userId);
            try {
                alarmDomainService.finalizeAlarm(alarm.getId(), request.getResult(), FireAlarm.SOURCE_FIELD,
                        userId, userName, request.getRemark(), request.getPic1(), request.getPic2(), request.getPic3());
            } catch (BizException ex) {
                // 报警已被其它入口收口时，仍允许工单正常完成，避免工单永久卡在处理中
                if (!"该报警已处理".equals(ex.getMessage())) {
                    throw ex;
                }
                log.warn("alarm already finalized before work order finish, alarmId={}", alarm.getId());
            }
        }
        LocalDateTime now = LocalDateTime.now();
        int rows = baseMapper.update(null, new LambdaUpdateWrapper<WorkOrder>()
                .eq(WorkOrder::getId, order.getId())
                .eq(WorkOrder::getStatus, WorkOrder.STATUS_PROCESSING)
                .set(WorkOrder::getStatus, WorkOrder.STATUS_FINISHED)
                .set(WorkOrder::getFinishTime, now)
                .set(WorkOrder::getFinishRemark, truncate(request.getRemark(), 500))
                .set(WorkOrder::getFinishPic1, request.getPic1())
                .set(WorkOrder::getFinishPic2, request.getPic2())
                .set(WorkOrder::getFinishPic3, request.getPic3())
                .set(WorkOrder::getUpdateTime, now));
        if (rows == 0) {
            throw new BizException("工单已完成，请勿重复提交");
        }
    }

    private void fillExtra(List<WorkOrderVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Set<Long> merchantIds = records.stream().map(WorkOrderVO::getMerchantId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> merchantNames = merchantIds.isEmpty() ? Collections.emptyMap()
                : merchantMapper.selectBatchIds(merchantIds).stream()
                .collect(Collectors.toMap(Merchant::getId, Merchant::getName, (a, b) -> a));
        Set<Long> workAreaIds = records.stream().map(WorkOrderVO::getWorkAreaId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> workAreaNames = workAreaIds.isEmpty() ? Collections.emptyMap()
                : workAreaMapper.selectBatchIds(workAreaIds).stream()
                .collect(Collectors.toMap(SysWorkArea::getId, SysWorkArea::getName, (a, b) -> a));
        Set<Long> alarmIds = records.stream().map(WorkOrderVO::getAlarmId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, FireAlarm> alarms = alarmIds.isEmpty() ? Collections.emptyMap()
                : alarmMapper.selectBatchIds(alarmIds).stream()
                .collect(Collectors.toMap(FireAlarm::getId, Function.identity(), (a, b) -> a));
        LocalDateTime now = LocalDateTime.now();
        for (WorkOrderVO vo : records) {
            vo.setMerchantName(merchantNames.get(vo.getMerchantId()));
            vo.setWorkAreaName(workAreaNames.get(vo.getWorkAreaId()));
            FireAlarm alarm = alarms.get(vo.getAlarmId());
            if (alarm != null) {
                vo.setAlarmSn(alarm.getSn());
                vo.setAlarmTime(alarm.getAlarmTime());
                vo.setAlarmLevel(alarm.getAlarmLevel());
                vo.setAlarmResult(alarm.getResult());
                vo.setDangerId(alarm.getDangerId());
                vo.setDangerStatus(resolveDangerStatus(alarm));
                if (alarm.getDangerConvertDeadline() != null && alarm.getDangerId() == null
                        && !isSelfRecovered(alarm)) {
                    long remain = Duration.between(now, alarm.getDangerConvertDeadline()).getSeconds();
                    vo.setDangerRemainSeconds(Math.max(0L, remain));
                }
            } else {
                vo.setDangerStatus(DANGER_NONE);
            }
        }
    }

    private String resolveDangerStatus(FireAlarm alarm) {
        if (!Integer.valueOf(FireAlarm.RESULT_FAULT).equals(alarm.getResult())) {
            return DANGER_NONE;
        }
        if (alarm.getDangerId() != null) {
            return DANGER_CONVERTED;
        }
        if (isSelfRecovered(alarm)) {
            return DANGER_RECOVERED;
        }
        return alarm.getDangerConvertDeadline() == null ? DANGER_NONE : DANGER_PENDING;
    }

    private boolean isSelfRecovered(FireAlarm alarm) {
        return alarm.getDangerConvertDeadline() != null && alarm.getRecoverTime() != null
                && !alarm.getRecoverTime().isAfter(alarm.getDangerConvertDeadline());
    }

    private WorkOrder require(Long id) {
        WorkOrder order = dataScopeService.isPlatform()
                ? dataScopeService.withIgnoreTenant(() -> baseMapper.selectById(id))
                : baseMapper.selectById(id);
        if (order == null || !dataScopeService.canAccess(order.getTenantId())) {
            throw new BizException("工单不存在");
        }
        if (SysRole.SCOPE_WORK_AREA.equals(TenantContext.getDataScope())
                && !Objects.equals(order.getHandlerId(), currentUserId())) {
            throw new BizException("工单不存在");
        }
        return order;
    }

    protected Long currentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    private String currentUserName(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        return StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUserName();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
