package com.fm.module.alarm.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fm.common.constant.TimeZones;
import com.fm.common.context.TenantContext;
import com.fm.common.exception.BizException;
import com.fm.module.alarm.converter.AlarmConverter;
import com.fm.module.alarm.dto.AlarmCallDTO;
import com.fm.module.alarm.dto.AlarmDispatchDTO;
import com.fm.module.alarm.dto.AlarmMerchantHandleDTO;
import com.fm.module.alarm.dto.AlarmQueryDTO;
import com.fm.module.alarm.dto.AlarmVerifyDTO;
import com.fm.module.alarm.entity.AlarmLevel;
import com.fm.module.alarm.entity.CallRecord;
import com.fm.module.alarm.entity.FireAlarm;
import com.fm.module.alarm.entity.FireAlarmTrace;
import com.fm.module.alarm.event.AlarmCreatedEvent;
import com.fm.module.alarm.mapper.CallRecordMapper;
import com.fm.module.alarm.mapper.FireAlarmMapper;
import com.fm.module.alarm.mapper.FireAlarmTraceMapper;
import com.fm.module.alarm.mq.AlarmPayload;
import com.fm.module.alarm.mq.DeviceMessage;
import com.fm.module.alarm.mq.MqConstants;
import com.fm.module.alarm.service.AlarmDomainService;
import com.fm.module.alarm.service.AlarmService;
import com.fm.module.alarm.vo.AlarmContactVO;
import com.fm.module.alarm.vo.AlarmDispatchOptionVO;
import com.fm.module.alarm.vo.AlarmKpiVO;
import com.fm.module.alarm.vo.AlarmMerchantVO;
import com.fm.module.alarm.vo.AlarmVO;
import com.fm.module.device.entity.Device;
import com.fm.module.device.entity.Merchant;
import com.fm.module.device.entity.MerchantFloor;
import com.fm.module.device.mapper.DeviceMapper;
import com.fm.module.device.mapper.MerchantFloorMapper;
import com.fm.module.device.mapper.MerchantMapper;
import com.fm.module.device.service.DictService;
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
import com.fm.module.workorder.entity.WorkOrder;
import com.fm.module.workorder.mapper.WorkOrderMapper;
import com.fm.module.workorder.service.WorkOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 报警域服务实现：MQ 幂等入库、收口、商户处置、值守复核、派单与故障转隐患。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmServiceImpl implements AlarmService {

    private static final long DEFAULT_PAGE_SIZE = 10L;
    private static final long MAX_PAGE_SIZE = 100L;
    private static final String USER_TYPE_MERCHANT = "MERCHANT";
    private static final String USER_TYPE_TENANT = "TENANT";
    private static final String CALL_TYPE_ALERT = "电话告警";
    private static final String CALL_TYPE_VERIFY = "电话核警";
    private static final List<Integer> HANDLING_TRACE_TYPES = Collections.unmodifiableList(Arrays.asList(
            FireAlarmTrace.TYPE_CALL_ALARM, FireAlarmTrace.TYPE_CALL_VERIFY,
            FireAlarmTrace.TYPE_DISPATCH, FireAlarmTrace.TYPE_VERIFY,
            FireAlarmTrace.TYPE_MERCHANT_HANDLE, FireAlarmTrace.TYPE_CONVERT_DANGER));

    private final FireAlarmMapper alarmMapper;
    private final FireAlarmTraceMapper traceMapper;
    private final CallRecordMapper callRecordMapper;
    private final DeviceMapper deviceMapper;
    private final MerchantMapper merchantMapper;
    private final MerchantFloorMapper floorMapper;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysWorkAreaMapper workAreaMapper;
    private final SysWorkAreaMerchantMapper workAreaMerchantMapper;
    private final SysUserWorkAreaMapper userWorkAreaMapper;
    private final WorkOrderMapper workOrderMapper;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final DataScopeService dataScopeService;
    private final AlarmDomainService alarmDomainService;
    private final WorkOrderService workOrderService;
    private final DictService dictService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleAlarm(DeviceMessage message) {
        if (message.getTenantId() == null) {
            log.warn("alarm message without tenantId dropped, messageId={}", message.getMessageId());
            return;
        }
        TenantContext.set(message.getTenantId(), "MQ", "ALL", false);
        try {
            Device device = findDevice(message.getSn());
            if (device == null) {
                log.warn("unregistered device alarm dropped, sn={}, messageId={}",
                        message.getSn(), message.getMessageId());
                return;
            }
            AlarmPayload payload = objectMapper.convertValue(message.getPayload(), AlarmPayload.class);
            Integer loopNo = intValue(payload.getData(), "loopNo");
            Integer nodeNo = intValue(payload.getData(), "nodeNo");
            Integer channelId = intValue(payload.getData(), "channelId");
            if (isDuplicateAlarm(message, loopNo, nodeNo, channelId, payload.getAlarmType())) {
                log.info("duplicate alarm ignored, sn={}, loop={}, node={}, messageId={}",
                        message.getSn(), loopNo, nodeNo, message.getMessageId());
                return;
            }
            FireAlarm alarm = new FireAlarm();
            alarm.setTenantId(message.getTenantId());
            alarm.setMessageId(message.getMessageId());
            alarm.setSn(message.getSn());
            alarm.setDeviceType(device.getType());
            alarm.setMerchantId(device.getMerchantId());
            alarm.setPosition(device.getPosition());
            alarm.setAlarmTime(toTime(message.getEventTime()));
            int level = resolveLevel(payload);
            alarm.setAlarmType(level == AlarmLevel.FAULT ? "FAULT" : "FIRE");
            alarm.setAlarmLevel(level);
            if (level == AlarmLevel.FAULT) {
                // 设备故障从上报时间起满 1 小时自动转隐患，不要求先收口
                alarm.setDangerConvertDeadline(alarm.getAlarmTime().plusHours(1));
            }
            alarm.setEventCode(payload.getEventCode());
            alarm.setLoopNo(loopNo);
            alarm.setNodeNo(nodeNo);
            alarm.setChannelId(channelId);
            alarm.setSnapshotUrl(textValue(payload.getData(), "snapshotUrl"));
            alarm.setReportData(payload.getData() == null ? null : payload.getData().toString());
            alarm.setRawData(payload.getRaw());
            alarm.setStatus(FireAlarm.STATUS_PENDING);
            alarm.setVerifyStatus(FireAlarm.VERIFY_PENDING);
            alarm.setCreateTime(LocalDateTime.now());
            alarm.setUpdateTime(LocalDateTime.now());
            try {
                alarmMapper.insert(alarm);
            } catch (DuplicateKeyException ex) {
                // 查后写存在并发窗口，唯一索引冲突时按重复告警忽略
                log.info("duplicate alarm ignored by unique key, sn={}, messageId={}",
                        message.getSn(), message.getMessageId());
                return;
            }
            FireAlarmTrace trace = new FireAlarmTrace();
            trace.setTenantId(alarm.getTenantId());
            trace.setAlarmId(alarm.getId());
            trace.setTraceType(FireAlarmTrace.TYPE_DEVICE);
            trace.setResult(1);
            trace.setContent("设备上报：" + describe(alarm));
            trace.setDetail(alarm.getReportData());
            trace.setMessageId(message.getMessageId());
            trace.setCreateTime(LocalDateTime.now());
            traceMapper.insert(trace);
            eventPublisher.publishEvent(new AlarmCreatedEvent(alarm));
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleRecover(DeviceMessage message) {
        if (message.getTenantId() == null) {
            log.warn("recover message without tenantId dropped, messageId={}", message.getMessageId());
            return;
        }
        TenantContext.set(message.getTenantId(), "MQ", "ALL", false);
        try {
            if (message.getMessageId() != null) {
                Long sameMessage = traceMapper.selectCount(new LambdaQueryWrapper<FireAlarmTrace>()
                        .eq(FireAlarmTrace::getMessageId, message.getMessageId()));
                if (sameMessage != null && sameMessage > 0) {
                    log.info("duplicate recover message ignored, messageId={}", message.getMessageId());
                    return;
                }
            }
            AlarmPayload payload = objectMapper.convertValue(message.getPayload(), AlarmPayload.class);
            Integer loopNo = intValue(payload.getData(), "loopNo");
            Integer nodeNo = intValue(payload.getData(), "nodeNo");
            Integer channelId = intValue(payload.getData(), "channelId");
            // 同一设备可能同时存在火警与故障，带 alarmType 的恢复只销对应类型，避免销错单
            String alarmType = payload.getAlarmType();
            boolean filterByAlarmType = StringUtils.hasText(alarmType)
                    && !MqConstants.TYPE_RECOVER.equals(alarmType);
            FireAlarm alarm = alarmMapper.selectOne(new LambdaQueryWrapper<FireAlarm>()
                    .eq(FireAlarm::getSn, message.getSn())
                    .eq(filterByAlarmType, FireAlarm::getAlarmType, alarmType)
                    .eq(loopNo != null, FireAlarm::getLoopNo, loopNo)
                    .eq(nodeNo != null, FireAlarm::getNodeNo, nodeNo)
                    .eq(channelId != null, FireAlarm::getChannelId, channelId)
                    .isNull(FireAlarm::getRecoverTime)
                    .orderByDesc(FireAlarm::getAlarmTime)
                    .last("limit 1"));
            if (alarm == null) {
                log.warn("recover without pending alarm, sn={}, messageId={}",
                        message.getSn(), message.getMessageId());
                return;
            }
            FireAlarmTrace trace = new FireAlarmTrace();
            trace.setTenantId(alarm.getTenantId());
            trace.setAlarmId(alarm.getId());
            trace.setTraceType(FireAlarmTrace.TYPE_RECOVER);
            trace.setResult(1);
            trace.setContent("设备恢复：" + message.getSn());
            trace.setMessageId(message.getMessageId());
            trace.setCreateTime(LocalDateTime.now());
            traceMapper.insert(trace);
            alarmMapper.update(null, new LambdaUpdateWrapper<FireAlarm>()
                    .eq(FireAlarm::getId, alarm.getId())
                    .set(FireAlarm::getRecoverTime, toTime(message.getEventTime()))
                    .set(FireAlarm::getUpdateTime, LocalDateTime.now()));
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    public Page<AlarmVO> pageVO(AlarmQueryDTO query) {
        long pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        long pageSize = query.getPageSize() == null || query.getPageSize() < 1
                ? DEFAULT_PAGE_SIZE : Math.min(query.getPageSize(), MAX_PAGE_SIZE);
        LambdaQueryWrapper<FireAlarm> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(query.getMerchantId() != null, FireAlarm::getMerchantId, query.getMerchantId());
        wrapper.like(StringUtils.hasText(query.getSn()), FireAlarm::getSn, query.getSn());
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(FireAlarm::getSn, query.getKeyword())
                    .or().like(FireAlarm::getPosition, query.getKeyword()));
        }
        wrapper.eq(StringUtils.hasText(query.getAlarmType()), FireAlarm::getAlarmType, query.getAlarmType());
        wrapper.eq(query.getLevel() != null, FireAlarm::getAlarmLevel, query.getLevel());
        wrapper.eq(query.getStatus() != null, FireAlarm::getStatus, query.getStatus());
        wrapper.eq(query.getResult() != null, FireAlarm::getResult, query.getResult());
        wrapper.eq(query.getVerifyStatus() != null, FireAlarm::getVerifyStatus, query.getVerifyStatus());
        wrapper.ne(Boolean.TRUE.equals(query.getExcludeVerified()),
                FireAlarm::getVerifyStatus, FireAlarm.VERIFY_DONE);
        wrapper.ge(query.getStartTime() != null, FireAlarm::getAlarmTime, query.getStartTime());
        wrapper.le(query.getEndTime() != null, FireAlarm::getAlarmTime, query.getEndTime());
        wrapper.orderByDesc(FireAlarm::getAlarmTime);

        Page<FireAlarm> page = new Page<>(pageNum, pageSize);
        if (!applyPageScope(wrapper)) {
            return emptyPage(pageNum, pageSize);
        }
        Page<AlarmVO> voPage = AlarmConverter.toVOPage(dataScopeService.isPlatform()
                ? dataScopeService.withIgnoreTenant(() -> alarmMapper.selectPage(page, wrapper))
                : alarmMapper.selectPage(page, wrapper));
        fillAlarmExtra(voPage.getRecords());
        return voPage;
    }

    /**
     * 追加数据范围；外勤按工作区商户收窄，返回 false 表示没有可见商户。
     */
    private boolean applyPageScope(LambdaQueryWrapper<FireAlarm> wrapper) {
        if (dataScopeService.isPlatform()) {
            List<Long> tenantIds = dataScopeService.authorizedTenantIds();
            if (tenantIds.isEmpty()) {
                return false;
            }
            wrapper.in(FireAlarm::getTenantId, tenantIds);
            return true;
        }
        if (SysRole.SCOPE_WORK_AREA.equals(TenantContext.getDataScope())) {
            List<Long> merchantIds = dataScopeService.workAreaMerchantIds(currentUserId());
            if (merchantIds.isEmpty()) {
                return false;
            }
            wrapper.in(FireAlarm::getMerchantId, merchantIds);
        }
        return true;
    }

    private Page<AlarmVO> emptyPage(long pageNum, long pageSize) {
        return new Page<>(pageNum, pageSize, 0);
    }

    @Override
    public AlarmVO detail(Long id) {
        FireAlarm alarm = requireVisible(id);
        AlarmVO vo = AlarmConverter.toVO(alarm);
        fillAlarmExtra(Collections.singletonList(vo));
        return vo;
    }

    @Override
    public List<FireAlarmTrace> timeline(Long alarmId) {
        requireVisible(alarmId);
        LambdaQueryWrapper<FireAlarmTrace> wrapper = new LambdaQueryWrapper<FireAlarmTrace>()
                .eq(FireAlarmTrace::getAlarmId, alarmId)
                .orderByAsc(FireAlarmTrace::getCreateTime);
        return dataScopeService.isPlatform()
                ? dataScopeService.withIgnoreTenant(() -> traceMapper.selectList(wrapper))
                : traceMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void verify(AlarmVerifyDTO request) {
        FireAlarm alarm = requireVisible(request.getId());
        if (AlarmDomainService.isFault(alarm)) {
            throw new BizException("设备故障报警由转隐患流程处理，无需核警");
        }
        if (Integer.valueOf(FireAlarm.STATUS_HANDLED).equals(alarm.getStatus())) {
            throw new BizException("该报警已处理");
        }
        ensureNoActiveWorkOrder(alarm);
        Long userId = currentUserId();
        String userName = currentUserName(userId);
        alarmDomainService.finalizeAlarm(alarm.getId(), request.getResult(), FireAlarm.SOURCE_DUTY,
                userId, userName, request.getRemark(), request.getPic1(), request.getPic2(), request.getPic3());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void merchantHandle(AlarmMerchantHandleDTO request) {
        if (!Integer.valueOf(FireAlarm.RESULT_FALSE).equals(request.getResult())
                && !Integer.valueOf(FireAlarm.RESULT_TRUE).equals(request.getResult())) {
            throw new BizException("处置结论只能是误报或火警");
        }
        requireMerchantOperator();
        FireAlarm alarm = requireVisible(request.getId());
        if (AlarmDomainService.isFault(alarm)) {
            throw new BizException("设备故障报警由转隐患流程处理，无需商户处置");
        }
        Long merchantId = TenantContext.getMerchantId();
        if (alarm.getMerchantId() == null || !alarm.getMerchantId().equals(merchantId)) {
            throw new BizException(403, "只能处置本商户的报警");
        }
        if (Integer.valueOf(FireAlarm.STATUS_HANDLED).equals(alarm.getStatus())) {
            throw new BizException("该报警已处理");
        }
        ensureNoActiveWorkOrder(alarm);
        Long userId = currentUserId();
        String userName = currentUserName(userId);
        LocalDateTime now = LocalDateTime.now();

        if (request.getResult() == FireAlarm.RESULT_TRUE) {
            markMerchantFire(alarm, request, userId, userName, now);
            return;
        }
        if (Integer.valueOf(FireAlarm.RESULT_TRUE).equals(alarm.getResult())
                && FireAlarm.SOURCE_MERCHANT.equals(alarm.getHandleSource())) {
            throw new BizException("该报警已提交值守复核，请等待处理");
        }
        String content = "商户处置：误报，直接收口";
        alarmDomainService.appendTrace(alarm, FireAlarmTrace.TYPE_MERCHANT_HANDLE, 1, content, null);
        alarmDomainService.finalizeAlarm(alarm.getId(), request.getResult(), FireAlarm.SOURCE_MERCHANT,
                userId, userName, request.getRemark(), request.getPic1(), request.getPic2(), request.getPic3());
        alarmMapper.update(null, new LambdaUpdateWrapper<FireAlarm>()
                .eq(FireAlarm::getId, alarm.getId())
                .set(FireAlarm::getMerchantHandleResult, request.getResult())
                .set(FireAlarm::getMerchantHandleTime, now)
                .set(FireAlarm::getMerchantHandleBy, userId)
                .set(FireAlarm::getUpdateTime, now));
    }

    /**
     * 商户报火警：保持未处理，进入待值守复核。
     */
    private void markMerchantFire(FireAlarm alarm, AlarmMerchantHandleDTO request,
                                  Long userId, String userName, LocalDateTime now) {
        int rows = alarmMapper.update(null, new LambdaUpdateWrapper<FireAlarm>()
                .eq(FireAlarm::getId, alarm.getId())
                .eq(FireAlarm::getStatus, FireAlarm.STATUS_PENDING)
                .and(w -> w.isNull(FireAlarm::getHandleSource)
                        .or().ne(FireAlarm::getHandleSource, FireAlarm.SOURCE_MERCHANT))
                .set(FireAlarm::getStatus, FireAlarm.STATUS_PENDING)
                .set(FireAlarm::getResult, FireAlarm.RESULT_TRUE)
                .set(FireAlarm::getVerifyStatus, FireAlarm.VERIFY_DOING)
                .set(FireAlarm::getHandleSource, FireAlarm.SOURCE_MERCHANT)
                .set(FireAlarm::getMerchantHandleResult, FireAlarm.RESULT_TRUE)
                .set(FireAlarm::getMerchantHandleTime, now)
                .set(FireAlarm::getMerchantHandleBy, userId)
                .set(FireAlarm::getOpUserId, userId)
                .set(FireAlarm::getOpUserName, userName)
                .set(FireAlarm::getAddRemark, request.getRemark())
                .set(FireAlarm::getAddPic1, request.getPic1())
                .set(FireAlarm::getAddPic2, request.getPic2())
                .set(FireAlarm::getAddPic3, request.getPic3())
                .set(FireAlarm::getUpdateTime, now));
        if (rows == 0) {
            throw new BizException("该报警已提交复核，请勿重复提交");
        }
        Map<String, Object> detail = new HashMap<>(4);
        detail.put("result", FireAlarm.RESULT_TRUE);
        detail.put("remark", request.getRemark());
        detail.put("pic1", request.getPic1());
        alarmDomainService.appendTrace(alarm, FireAlarmTrace.TYPE_MERCHANT_HANDLE, 1,
                "商户报火警，待值守复核（" + userName + "）", detail);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dispatch(AlarmDispatchDTO request) {
        FireAlarm alarm = requireVisible(request.getId());
        if (AlarmDomainService.isFault(alarm)) {
            throw new BizException("故障报警只能转隐患，不能派单核警");
        }
        if (Integer.valueOf(FireAlarm.STATUS_HANDLED).equals(alarm.getStatus())) {
            throw new BizException("该报警已处理，不能派单");
        }
        if (Integer.valueOf(FireAlarm.VERIFY_DONE).equals(alarm.getVerifyStatus())) {
            throw new BizException("该报警已核警，不能派单");
        }
        Long exists = workOrderMapper.selectCount(new LambdaQueryWrapper<WorkOrder>()
                .eq(WorkOrder::getAlarmId, alarm.getId()));
        if (exists != null && exists > 0) {
            throw new BizException("该报警已派单");
        }
        SysWorkArea workArea = workAreaMapper.selectById(request.getWorkAreaId());
        if (workArea == null) {
            throw new BizException("工作区不存在");
        }
        if (!alarm.getTenantId().equals(workArea.getTenantId())) {
            throw new BizException(403, "工作区不属于当前运营中心");
        }
        SysUser handler = userMapper.selectById(request.getHandlerId());
        if (handler == null || !USER_TYPE_TENANT.equals(handler.getUserType())
                || !Integer.valueOf(1).equals(handler.getStatus())) {
            throw new BizException("外勤不存在或已停用");
        }
        if (!Objects.equals(handler.getTenantId(), workArea.getTenantId())) {
            throw new BizException(403, "外勤不属于该运营中心");
        }
        if (!isWorkAreaMember(workArea.getId(), handler.getId())) {
            throw new BizException("所选外勤不属于该工作区");
        }
        if (!isFieldUser(handler.getId(), workArea.getTenantId())) {
            throw new BizException("所选人员不是外勤");
        }
        Long merchantBound = workAreaMerchantMapper.selectCount(new LambdaQueryWrapper<SysWorkAreaMerchant>()
                .eq(SysWorkAreaMerchant::getWorkAreaId, workArea.getId())
                .eq(SysWorkAreaMerchant::getMerchantId, alarm.getMerchantId()));
        if (merchantBound == null || merchantBound == 0) {
            throw new BizException("报警商户不属于所选工作区");
        }
        Long userId = currentUserId();
        String userName = currentUserName(userId);
        String handlerName = StringUtils.hasText(handler.getRealName()) ? handler.getRealName() : handler.getUserName();
        LocalDateTime now = LocalDateTime.now();
        int rows = alarmMapper.update(null, new LambdaUpdateWrapper<FireAlarm>()
                .eq(FireAlarm::getId, alarm.getId())
                .eq(FireAlarm::getStatus, FireAlarm.STATUS_PENDING)
                .isNull(FireAlarm::getWorkAreaId)
                .set(FireAlarm::getWorkAreaId, workArea.getId())
                .set(FireAlarm::getVerifyStatus, FireAlarm.VERIFY_DOING)
                .set(FireAlarm::getDispatchUser, userName)
                .set(FireAlarm::getDispatchTime, now)
                .set(FireAlarm::getDispatchRemark, request.getDispatchRemark())
                .set(FireAlarm::getOpUserId, userId)
                .set(FireAlarm::getOpUserName, userName)
                .set(FireAlarm::getUpdateTime, now));
        if (rows == 0) {
            throw new BizException("该报警已处理，不能派单");
        }
        String position = StringUtils.hasText(alarm.getPosition()) ? alarm.getPosition() : alarm.getSn();
        String title = "报警工单：" + levelText(alarm.getAlarmLevel()) + " " + position;
        String content = "SN：" + alarm.getSn() + "；位置：" + position
                + "；时间：" + alarm.getAlarmTime() + "；备注：" + safe(request.getDispatchRemark());
        workOrderService.createFromAlarm(alarm.getTenantId(), alarm.getMerchantId(), alarm.getId(),
                workArea.getId(), title, content,
                alarm.getAlarmLevel() != null && alarm.getAlarmLevel() == AlarmLevel.FIRE ? 2 : 1,
                handler.getId(), handlerName);
        Map<String, Object> detail = new HashMap<>(8);
        detail.put("workAreaId", workArea.getId());
        detail.put("workAreaName", workArea.getName());
        detail.put("handlerId", handler.getId());
        detail.put("handlerName", handlerName);
        detail.put("dispatchRemark", request.getDispatchRemark());
        alarmDomainService.appendTrace(alarm, FireAlarmTrace.TYPE_DISPATCH, 1,
                "派单给 " + handlerName + "（" + workArea.getName() + "）", detail);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long convertDanger(Long alarmId) {
        requireVisible(alarmId);
        Long userId = currentUserId();
        return alarmDomainService.convertDanger(alarmId, "MANUAL", userId, currentUserName(userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void call(AlarmCallDTO request) {
        FireAlarm alarm = requireVisible(request.getId());
        if (AlarmDomainService.isFault(alarm)) {
            throw new BizException("故障报警只能转隐患，无需电话告警");
        }
        if (!StringUtils.hasText(request.getCalledNo())) {
            throw new BizException("被叫号码不能为空");
        }
        boolean alertCall = Integer.valueOf(1).equals(request.getCallType());
        boolean connected = !Boolean.FALSE.equals(request.getConnected());
        String callTypeText = alertCall ? CALL_TYPE_ALERT : CALL_TYPE_VERIFY;
        String callState = StringUtils.hasText(request.getCallState())
                ? request.getCallState() : (connected ? "dealing" : "notDeal");
        LocalDateTime now = LocalDateTime.now();
        CallRecord existing = StringUtils.hasText(request.getActionId())
                ? callRecordMapper.selectOne(new LambdaQueryWrapper<CallRecord>()
                        .eq(CallRecord::getActionId, request.getActionId()))
                : null;
        if (existing == null) {
            CallRecord record = new CallRecord();
            record.setTenantId(alarm.getTenantId());
            record.setAlarmId(alarm.getId());
            record.setCalledNo(request.getCalledNo());
            record.setCallState(callState);
            record.setCallBegin(now);
            record.setSynced(0);
            record.setCreateTime(now);
            record.setUpdateTime(now);
            callRecordMapper.insert(record);
        } else {
            CallRecord update = new CallRecord();
            update.setId(existing.getId());
            update.setCallState(callState);
            update.setUpdateTime(now);
            callRecordMapper.updateById(update);
        }
        String contactText = StringUtils.hasText(request.getContactName())
                ? "（" + request.getContactName() + "）" : "";
        StringBuilder content = new StringBuilder(callTypeText)
                .append("：拨打 ").append(safe(request.getCalledNo())).append(contactText)
                .append("，").append(connected ? "已接通" : "未接通");
        if (alertCall && !connected) {
            content.append("，需派单核警");
        }
        if (StringUtils.hasText(request.getRemark())) {
            content.append("；").append(request.getRemark());
        }
        Map<String, Object> detail = new HashMap<>(8);
        detail.put("calledNo", request.getCalledNo());
        detail.put("callState", callState);
        detail.put("contactName", request.getContactName());
        detail.put("callType", callTypeText);
        detail.put("connected", connected);
        detail.put("actionId", request.getActionId());
        detail.put("remark", request.getRemark());
        alarmDomainService.appendTrace(alarm,
                alertCall ? FireAlarmTrace.TYPE_CALL_ALARM : FireAlarmTrace.TYPE_CALL_VERIFY,
                connected ? 1 : 2, content.toString(), detail);
    }

    @Override
    public AlarmKpiVO kpi() {
        Long unhandledFire = count(new LambdaQueryWrapper<FireAlarm>()
                .eq(FireAlarm::getStatus, FireAlarm.STATUS_PENDING)
                .eq(FireAlarm::getAlarmLevel, AlarmLevel.FIRE));
        Long pendingReview = count(new LambdaQueryWrapper<FireAlarm>()
                .eq(FireAlarm::getStatus, FireAlarm.STATUS_PENDING)
                .eq(FireAlarm::getVerifyStatus, FireAlarm.VERIFY_DOING)
                .eq(FireAlarm::getHandleSource, FireAlarm.SOURCE_MERCHANT)
                .eq(FireAlarm::getResult, FireAlarm.RESULT_TRUE));
        Long verifying = count(new LambdaQueryWrapper<FireAlarm>()
                .eq(FireAlarm::getStatus, FireAlarm.STATUS_PENDING)
                .eq(FireAlarm::getVerifyStatus, FireAlarm.VERIFY_DOING));
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        Long todayHandled = count(new LambdaQueryWrapper<FireAlarm>()
                .eq(FireAlarm::getAlarmLevel, AlarmLevel.FIRE)
                .eq(FireAlarm::getStatus, FireAlarm.STATUS_HANDLED)
                .ge(FireAlarm::getFinalizeTime, start)
                .lt(FireAlarm::getFinalizeTime, end));
        Double avgSeconds = dataScopeService.isPlatform()
                ? dataScopeService.withIgnoreTenant(() -> alarmMapper.avgResponseSeconds(start, end))
                : alarmMapper.avgResponseSeconds(start, end);
        double minutes = avgSeconds == null ? 0D : avgSeconds / 60D;
        return AlarmConverter.toKpiVO(unhandledFire, pendingReview, verifying, todayHandled,
                Math.round(minutes * 10D) / 10D);
    }

    @Override
    public List<AlarmMerchantVO> merchantList(String keyword, Boolean unverifiedOnly) {
        boolean onlyUnverified = Boolean.TRUE.equals(unverifiedOnly);
        List<AlarmMerchantVO> stats = dataScopeService.isPlatform()
                ? dataScopeService.withIgnoreTenant(() -> countMerchantStats(onlyUnverified))
                : countMerchantStats(onlyUnverified);
        Map<Long, AlarmMerchantVO> statMap = stats.stream()
                .collect(Collectors.toMap(AlarmMerchantVO::getId, Function.identity(), (a, b) -> a));
        if (onlyUnverified && statMap.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<Merchant>()
                .orderByAsc(Merchant::getName)
                .last("limit 500");
        wrapper.like(StringUtils.hasText(keyword), Merchant::getName, keyword);
        if (onlyUnverified) {
            wrapper.in(Merchant::getId, statMap.keySet());
        }
        List<Merchant> merchants = dataScopeService.isPlatform()
                ? dataScopeService.withIgnoreTenant(() -> merchantMapper.selectList(wrapper))
                : merchantMapper.selectList(wrapper);
        List<AlarmMerchantVO> result = new ArrayList<>(merchants.size());
        for (Merchant merchant : merchants) {
            AlarmMerchantVO stat = statMap.get(merchant.getId());
            if (!onlyUnverified || stat != null) {
                result.add(AlarmConverter.toMerchantVO(merchant, stat));
            }
        }
        return result;
    }

    private List<AlarmMerchantVO> countMerchantStats(boolean onlyUnverified) {
        return onlyUnverified ? alarmMapper.countUnverifiedByMerchant() : alarmMapper.countPendingByMerchant();
    }

    @Override
    public List<AlarmDispatchOptionVO> dispatchOptions() {
        List<SysWorkArea> workAreas = workAreaMapper.selectList(new LambdaQueryWrapper<SysWorkArea>()
                .orderByAsc(SysWorkArea::getName));
        if (workAreas.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> workAreaIds = workAreas.stream().map(SysWorkArea::getId).collect(Collectors.toList());
        List<SysUserWorkArea> relations = userWorkAreaMapper.selectList(
                new LambdaQueryWrapper<SysUserWorkArea>().in(SysUserWorkArea::getWorkAreaId, workAreaIds));
        Set<Long> memberIds = relations.stream().map(SysUserWorkArea::getUserId).collect(Collectors.toSet());
        if (memberIds.isEmpty()) {
            return workAreas.stream().map(area -> AlarmConverter.toDispatchOptionVO(area, Collections.emptyList()))
                    .collect(Collectors.toList());
        }
        List<SysUser> members = userMapper.selectBatchIds(memberIds).stream()
                .filter(user -> USER_TYPE_TENANT.equals(user.getUserType()))
                .filter(user -> Integer.valueOf(1).equals(user.getStatus()))
                .collect(Collectors.toList());
        Set<Long> fieldUserIds = fieldUserIds(members.stream().map(SysUser::getId).collect(Collectors.toList()));
        Map<Long, SysUser> userMap = members.stream()
                .filter(user -> fieldUserIds.contains(user.getId()))
                .collect(Collectors.toMap(SysUser::getId, Function.identity(), (a, b) -> a));
        Map<Long, List<Long>> areaUserIds = relations.stream().collect(Collectors.groupingBy(
                SysUserWorkArea::getWorkAreaId,
                Collectors.mapping(SysUserWorkArea::getUserId, Collectors.toList())));
        List<AlarmDispatchOptionVO> options = new ArrayList<>(workAreas.size());
        for (SysWorkArea area : workAreas) {
            List<AlarmDispatchOptionVO.Handler> handlers = areaUserIds
                    .getOrDefault(area.getId(), Collections.emptyList()).stream()
                    .distinct()
                    .map(userMap::get)
                    .filter(Objects::nonNull)
                    .map(AlarmConverter::toHandler)
                    .collect(Collectors.toList());
            options.add(AlarmConverter.toDispatchOptionVO(area, handlers));
        }
        return options;
    }

    private Set<Long> fieldUserIds(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptySet();
        }
        SysRole fieldRole = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleKey, SysRole.TENANT_FIELD)
                .last("limit 1"));
        if (fieldRole == null) {
            return new HashSet<>(userIds);
        }
        return userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getRoleId, fieldRole.getId())
                        .in(SysUserRole::getUserId, userIds))
                .stream().map(SysUserRole::getUserId).collect(Collectors.toSet());
    }

    private boolean isFieldUser(Long userId, Long tenantId) {
        SysRole fieldRole = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleKey, SysRole.TENANT_FIELD)
                .eq(SysRole::getTenantId, tenantId)
                .last("limit 1"));
        if (fieldRole == null) {
            return true;
        }
        Long count = userRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId)
                .eq(SysUserRole::getRoleId, fieldRole.getId()));
        return count != null && count > 0;
    }

    private boolean isWorkAreaMember(Long workAreaId, Long userId) {
        Long count = userWorkAreaMapper.selectCount(new LambdaQueryWrapper<SysUserWorkArea>()
                .eq(SysUserWorkArea::getWorkAreaId, workAreaId)
                .eq(SysUserWorkArea::getUserId, userId));
        return count != null && count > 0;
    }

    private void fillAlarmExtra(List<AlarmVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Map<String, String> deviceTypeNames = dictService.deviceTypeNames(records.stream()
                .map(AlarmVO::getDeviceType).collect(Collectors.toSet()));
        Set<Long> merchantIds = records.stream().map(AlarmVO::getMerchantId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Merchant> merchants = merchantIds.isEmpty() ? Collections.emptyMap()
                : merchantMapper.selectBatchIds(merchantIds).stream()
                .collect(Collectors.toMap(Merchant::getId, Function.identity(), (a, b) -> a));
        Set<String> sns = records.stream().map(AlarmVO::getSn)
                .filter(StringUtils::hasText).collect(Collectors.toSet());
        Map<String, Device> devices = sns.isEmpty() ? Collections.emptyMap()
                : deviceMapper.selectList(new LambdaQueryWrapper<Device>().in(Device::getSn, sns)).stream()
                .filter(device -> StringUtils.hasText(device.getSn()))
                .collect(Collectors.toMap(Device::getSn, Function.identity(), (a, b) -> a));
        Set<Long> floorIds = devices.values().stream().map(Device::getFloorId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, MerchantFloor> floors = floorIds.isEmpty() ? Collections.emptyMap()
                : floorMapper.selectBatchIds(floorIds).stream()
                .collect(Collectors.toMap(MerchantFloor::getId, Function.identity(), (a, b) -> a));
        Set<Long> workAreaIds = records.stream().map(AlarmVO::getWorkAreaId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> workAreaNames = workAreaIds.isEmpty() ? Collections.emptyMap()
                : workAreaMapper.selectBatchIds(workAreaIds).stream()
                .collect(Collectors.toMap(SysWorkArea::getId, SysWorkArea::getName, (a, b) -> a));
        List<Long> alarmIds = records.stream().map(AlarmVO::getId).collect(Collectors.toList());
        Map<Long, WorkOrder> workOrders = workOrderMapper.selectList(new LambdaQueryWrapper<WorkOrder>()
                        .in(WorkOrder::getAlarmId, alarmIds)).stream()
                .collect(Collectors.toMap(WorkOrder::getAlarmId, Function.identity(), (a, b) -> a));
        LambdaQueryWrapper<FireAlarmTrace> traceQuery = new LambdaQueryWrapper<FireAlarmTrace>()
                .in(FireAlarmTrace::getAlarmId, alarmIds)
                .in(FireAlarmTrace::getTraceType, HANDLING_TRACE_TYPES)
                .orderByDesc(FireAlarmTrace::getCreateTime);
        List<FireAlarmTrace> traces = dataScopeService.isPlatform()
                ? dataScopeService.withIgnoreTenant(() -> traceMapper.selectList(traceQuery))
                : traceMapper.selectList(traceQuery);
        Map<Long, FireAlarmTrace> lastTraces = traces.stream()
                .collect(Collectors.toMap(FireAlarmTrace::getAlarmId, Function.identity(), (a, b) -> a));
        Set<Long> phoneAlertedIds = traces.stream()
                .filter(trace -> Objects.equals(trace.getTraceType(), FireAlarmTrace.TYPE_CALL_ALARM))
                .map(FireAlarmTrace::getAlarmId)
                .collect(Collectors.toSet());
        LocalDateTime now = LocalDateTime.now();
        for (AlarmVO vo : records) {
            vo.setDeviceTypeName(deviceTypeNames.get(vo.getDeviceType()));
            Merchant merchant = merchants.get(vo.getMerchantId());
            vo.setMerchantName(merchant == null ? null : merchant.getName());
            vo.setContacts(contactList(merchant));
            Device device = devices.get(vo.getSn());
            MerchantFloor floor = device == null || device.getFloorId() == null
                    ? null : floors.get(device.getFloorId());
            if (floor != null) {
                vo.setFloorName(floor.getName());
                vo.setFloorPlanUrl(floor.getFloorPlanUrl());
                vo.setFloorX(device.getFloorX());
                vo.setFloorY(device.getFloorY());
            }
            vo.setWorkAreaName(workAreaNames.get(vo.getWorkAreaId()));
            WorkOrder order = workOrders.get(vo.getId());
            if (order != null) {
                vo.setWorkOrderId(order.getId());
                vo.setWorkOrderStatus(order.getStatus());
            }
            FireAlarmTrace lastTrace = lastTraces.get(vo.getId());
            if (lastTrace != null) {
                vo.setLastTraceType(lastTrace.getTraceType());
                vo.setLastTraceResult(lastTrace.getResult());
                vo.setLastTraceContent(lastTrace.getContent());
                vo.setLastTraceTime(lastTrace.getCreateTime());
            }
            vo.setPhoneAlerted(phoneAlertedIds.contains(vo.getId()));
            vo.setSelfRecovered(isSelfRecovered(vo));
            if (vo.getDangerConvertDeadline() != null && vo.getDangerId() == null
                    && !Boolean.TRUE.equals(vo.getSelfRecovered())) {
                long remain = Duration.between(now, vo.getDangerConvertDeadline()).getSeconds();
                vo.setDangerRemainSeconds(Math.max(0L, remain));
            }
        }
    }

    private List<AlarmContactVO> contactList(Merchant merchant) {
        if (merchant == null) {
            return Collections.emptyList();
        }
        List<AlarmContactVO> contacts = new ArrayList<>(3);
        addContact(contacts, "业主", merchant.getOwnerName(), merchant.getPhone());
        addContact(contacts, "第一责任人", merchant.getContact1(), merchant.getPhone1());
        addContact(contacts, "第二联系人", merchant.getContact2(), merchant.getPhone2());
        return contacts;
    }

    private void addContact(List<AlarmContactVO> contacts, String relation, String name, String phone) {
        if (!StringUtils.hasText(phone)) {
            return;
        }
        AlarmContactVO contact = new AlarmContactVO();
        contact.setRelation(relation);
        contact.setName(StringUtils.hasText(name) ? name : relation);
        contact.setPhone(phone);
        contacts.add(contact);
    }

    private boolean isSelfRecovered(AlarmVO vo) {
        return vo.getDangerConvertDeadline() != null && vo.getRecoverTime() != null
                && !vo.getRecoverTime().isAfter(vo.getDangerConvertDeadline());
    }

    private FireAlarm requireVisible(Long id) {
        FireAlarm alarm = id == null ? null : alarmMapper.selectById(id);
        if (alarm == null) {
            throw new BizException("报警不存在");
        }
        if (SysRole.SCOPE_WORK_AREA.equals(TenantContext.getDataScope())) {
            List<Long> merchantIds = dataScopeService.workAreaMerchantIds(currentUserId());
            if (alarm.getMerchantId() == null || !merchantIds.contains(alarm.getMerchantId())) {
                throw new BizException("报警不存在");
            }
        }
        return alarm;
    }

    private void requireMerchantOperator() {
        if (!USER_TYPE_MERCHANT.equals(TenantContext.getUserType())) {
            throw new BizException(403, "仅商户账号可执行商户处置");
        }
    }

    /**
     * 存在待接单/处理中工单时，其它入口不能直接收口，避免工单永久卡在处理中。
     */
    private void ensureNoActiveWorkOrder(FireAlarm alarm) {
        Long active = workOrderMapper.selectCount(new LambdaQueryWrapper<WorkOrder>()
                .eq(WorkOrder::getAlarmId, alarm.getId())
                .in(WorkOrder::getStatus, WorkOrder.STATUS_PENDING, WorkOrder.STATUS_PROCESSING));
        if (active != null && active > 0) {
            throw new BizException("该报警已派单，请由外勤完成工单后收口");
        }
    }

    private Long count(LambdaQueryWrapper<FireAlarm> wrapper) {
        return dataScopeService.isPlatform()
                ? dataScopeService.withIgnoreTenant(() -> alarmMapper.selectCount(wrapper))
                : alarmMapper.selectCount(wrapper);
    }

    private Long currentUserId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception ex) {
            return null;
        }
    }

    private String currentUserName(Long userId) {
        if (userId == null) {
            return null;
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        return StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUserName();
    }

    private Device findDevice(String sn) {
        return deviceMapper.selectOne(new LambdaQueryWrapper<Device>().eq(Device::getSn, sn));
    }

    private boolean isDuplicateAlarm(DeviceMessage message, Integer loopNo, Integer nodeNo,
                                     Integer channelId, String alarmType) {
        if (message.getMessageId() != null) {
            Long sameMessage = alarmMapper.selectCount(new LambdaQueryWrapper<FireAlarm>()
                    .eq(FireAlarm::getMessageId, message.getMessageId()));
            if (sameMessage != null && sameMessage > 0) {
                return true;
            }
        }
        Long active = alarmMapper.selectCount(new LambdaQueryWrapper<FireAlarm>()
                .eq(FireAlarm::getSn, message.getSn())
                .eq(alarmType != null, FireAlarm::getAlarmType, alarmType)
                .eq(loopNo != null, FireAlarm::getLoopNo, loopNo)
                .eq(nodeNo != null, FireAlarm::getNodeNo, nodeNo)
                .eq(channelId != null, FireAlarm::getChannelId, channelId)
                .isNull(FireAlarm::getRecoverTime));
        return active != null && active > 0;
    }

    private Integer intValue(JsonNode data, String field) {
        if (data == null || data.isNull()) {
            return null;
        }
        JsonNode node = data.get(field);
        if (node == null || node.isNull() || !node.canConvertToInt()) {
            return null;
        }
        return node.asInt();
    }

    private String textValue(JsonNode data, String field) {
        if (data == null || data.isNull()) {
            return null;
        }
        JsonNode node = data.get(field);
        return node == null || node.isNull() ? null : node.asText();
    }

    private int resolveLevel(AlarmPayload payload) {
        if (payload.getLevel() != null) {
            return payload.getLevel() == AlarmLevel.FAULT ? AlarmLevel.FAULT : AlarmLevel.FIRE;
        }
        if ("FAULT".equals(payload.getAlarmType())) {
            return AlarmLevel.FAULT;
        }
        return AlarmLevel.FIRE;
    }

    private String describe(FireAlarm alarm) {
        String type = alarm.getAlarmLevel() == null ? "报警" : levelText(alarm.getAlarmLevel());
        return type + "，设备 " + alarm.getSn();
    }

    private String levelText(int level) {
        if (level == AlarmLevel.FAULT) {
            return "故障";
        }
        return "火警";
    }

    private LocalDateTime toTime(Long epochMillis) {
        if (epochMillis == null) {
            return LocalDateTime.now();
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), TimeZones.BUSINESS);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
