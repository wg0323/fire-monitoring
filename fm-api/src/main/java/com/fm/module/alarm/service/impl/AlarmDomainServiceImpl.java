package com.fm.module.alarm.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fm.common.exception.BizException;
import com.fm.module.alarm.entity.FireAlarm;
import com.fm.module.alarm.entity.FireAlarmTrace;
import com.fm.module.alarm.mapper.FireAlarmMapper;
import com.fm.module.alarm.mapper.FireAlarmTraceMapper;
import com.fm.module.alarm.service.AlarmDomainService;
import com.fm.module.danger.entity.Danger;
import com.fm.module.danger.entity.DangerTrace;
import com.fm.module.danger.mapper.DangerMapper;
import com.fm.module.danger.mapper.DangerTraceMapper;
import com.fm.module.system.entity.SysWorkAreaMerchant;
import com.fm.module.system.mapper.SysWorkAreaMerchantMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 报警领域实现：所有状态迁移都通过条件更新完成，保证并发与重复提交下结果唯一。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmDomainServiceImpl implements AlarmDomainService {

    private static final long CONVERT_WINDOW_HOURS = 1L;
    private static final String CONVERT_MANUAL = "MANUAL";
    private static final String CONVERT_AUTO = "AUTO";

    private final FireAlarmMapper alarmMapper;
    private final FireAlarmTraceMapper traceMapper;
    private final DangerMapper dangerMapper;
    private final DangerTraceMapper dangerTraceMapper;
    private final SysWorkAreaMerchantMapper workAreaMerchantMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finalizeAlarm(Long alarmId, Integer result, String source, Long operatorId, String operatorName,
                              String remark, String pic1, String pic2, String pic3) {
        validateResult(result);
        FireAlarm alarm = requireAlarm(alarmId);
        if (Integer.valueOf(FireAlarm.STATUS_HANDLED).equals(alarm.getStatus())) {
            throw new BizException("该报警已处理");
        }
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<FireAlarm> update = new LambdaUpdateWrapper<FireAlarm>()
                .eq(FireAlarm::getId, alarmId)
                .eq(FireAlarm::getStatus, FireAlarm.STATUS_PENDING)
                .set(FireAlarm::getStatus, FireAlarm.STATUS_HANDLED)
                .set(FireAlarm::getVerifyStatus, FireAlarm.VERIFY_DONE)
                .set(FireAlarm::getResult, result)
                .set(FireAlarm::getHandleSource, source)
                .set(FireAlarm::getHandlerId, operatorId)
                .set(FireAlarm::getHandlerName, operatorName)
                .set(FireAlarm::getFinalizeTime, now)
                .set(FireAlarm::getOpUserId, operatorId)
                .set(FireAlarm::getOpUserName, operatorName)
                .set(FireAlarm::getAddRemark, remark)
                .set(FireAlarm::getAddPic1, pic1)
                .set(FireAlarm::getAddPic2, pic2)
                .set(FireAlarm::getAddPic3, pic3)
                .set(FireAlarm::getUpdateTime, now);
        if (result == FireAlarm.RESULT_FAULT) {
            update.set(FireAlarm::getDangerConvertDeadline, convertDeadline(alarm));
        } else {
            update.set(FireAlarm::getDangerConvertDeadline, null);
        }
        int rows = alarmMapper.update(null, update);
        if (rows == 0) {
            throw new BizException("该报警已处理");
        }
        Map<String, Object> detail = new HashMap<>(8);
        detail.put("result", result);
        detail.put("source", source);
        detail.put("remark", remark);
        detail.put("pic1", pic1);
        detail.put("pic2", pic2);
        detail.put("pic3", pic3);
        appendTrace(alarm, FireAlarmTrace.TYPE_VERIFY, 1,
                "核警：" + resultText(result) + "（" + operatorName + "）", detail);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long convertDanger(Long alarmId, String convertSource, Long operatorId, String operatorName) {
        FireAlarm alarm = requireAlarm(alarmId);
        if (!AlarmDomainService.isFault(alarm)) {
            throw new BizException("仅故障报警可转隐患");
        }
        if (alarm.getDangerId() != null) {
            return alarm.getDangerId();
        }
        if (AlarmDomainService.isSelfRecovered(alarm)) {
            throw new BizException("该故障已自行恢复，不转隐患");
        }
        LocalDateTime now = LocalDateTime.now();
        Danger danger = buildDanger(alarm, operatorName, now);
        try {
            dangerMapper.insert(danger);
        } catch (DuplicateKeyException ex) {
            Danger existing = dangerMapper.selectOne(new LambdaQueryWrapper<Danger>()
                    .eq(Danger::getSourceAlarmId, alarmId));
            if (existing == null) {
                throw new BizException("隐患已生成，请刷新后查看");
            }
            linkDanger(alarm, existing.getId(), convertSource, operatorId, operatorName, now);
            return existing.getId();
        }
        int rows = linkDanger(alarm, danger.getId(), convertSource, operatorId, operatorName, now);
        if (rows == 0) {
            throw new BizException("该报警已转隐患");
        }
        return danger.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean recoverInWindow(Long alarmId) {
        FireAlarm alarm = requireAlarm(alarmId);
        if (alarm.getDangerId() != null || !AlarmDomainService.isSelfRecovered(alarm)) {
            return false;
        }
        int rows = alarmMapper.update(null, new LambdaUpdateWrapper<FireAlarm>()
                .eq(FireAlarm::getId, alarmId)
                .isNull(FireAlarm::getDangerId)
                .isNotNull(FireAlarm::getDangerConvertDeadline)
                .isNotNull(FireAlarm::getRecoverTime)
                .apply("recover_time <= danger_convert_deadline")
                .set(FireAlarm::getStatus, FireAlarm.STATUS_HANDLED)
                .set(FireAlarm::getVerifyStatus, FireAlarm.VERIFY_DONE)
                .set(FireAlarm::getResult, FireAlarm.RESULT_FAULT)
                .set(FireAlarm::getHandleSource, FireAlarm.SOURCE_SYSTEM)
                .set(FireAlarm::getFinalizeTime, alarm.getRecoverTime())
                .set(FireAlarm::getDangerConvertDeadline, null)
                .set(FireAlarm::getUpdateTime, LocalDateTime.now()));
        if (rows == 0) {
            return false;
        }
        Map<String, Object> detail = new HashMap<>(2);
        detail.put("recoverTime", alarm.getRecoverTime());
        detail.put("convertSource", "SELF_RECOVERED");
        appendTrace(alarm, FireAlarmTrace.TYPE_RECOVER, 1,
                "设备恢复：1 小时内自行恢复，不转隐患", detail);
        return true;
    }

    @Override
    public void appendTrace(FireAlarm alarm, int traceType, Integer result, String content, Object detail) {
        FireAlarmTrace trace = new FireAlarmTrace();
        trace.setTenantId(alarm.getTenantId());
        trace.setAlarmId(alarm.getId());
        trace.setTraceType(traceType);
        trace.setResult(result);
        trace.setContent(content);
        trace.setDetail(toJson(detail));
        trace.setCreateTime(LocalDateTime.now());
        traceMapper.insert(trace);
    }

    private Danger buildDanger(FireAlarm alarm, String operatorName, LocalDateTime now) {
        Danger danger = new Danger();
        danger.setTenantId(alarm.getTenantId());
        danger.setMerchantId(alarm.getMerchantId());
        danger.setSourceType(Danger.SOURCE_ALARM);
        danger.setSourceAlarmId(alarm.getId());
        danger.setAlarmId(alarm.getId());
        danger.setAlarmTime(alarm.getAlarmTime());
        danger.setConvertTime(now);
        danger.setStatus(Danger.STATUS_PENDING_ASSIGN);
        danger.setPosition(alarm.getPosition());
        danger.setRemark("设备故障转隐患：" + safe(alarm.getSn()) + " " + safe(alarm.getPosition()));
        danger.setDangerLevel(0);
        danger.setWorkAreaId(resolveWorkAreaId(alarm.getMerchantId()));
        danger.setCreateBy(operatorName);
        danger.setCreateTime(now);
        danger.setUpdateTime(now);
        return danger;
    }

    /**
     * 回写 danger_id 与收口字段；danger_id 条件更新保证同一条报警只绑定一条隐患。
     */
    private int linkDanger(FireAlarm alarm, Long dangerId, String convertSource,
                           Long operatorId, String operatorName, LocalDateTime now) {
        LambdaUpdateWrapper<FireAlarm> update = new LambdaUpdateWrapper<FireAlarm>()
                .eq(FireAlarm::getId, alarm.getId())
                .isNull(FireAlarm::getDangerId)
                .set(FireAlarm::getDangerId, dangerId)
                .set(FireAlarm::getUpdateTime, now);
        String handleSource = CONVERT_AUTO.equals(convertSource) ? FireAlarm.SOURCE_SYSTEM : manualHandleSource();
        if (!Integer.valueOf(FireAlarm.STATUS_HANDLED).equals(alarm.getStatus())) {
            update.set(FireAlarm::getStatus, FireAlarm.STATUS_HANDLED)
                    .set(FireAlarm::getVerifyStatus, FireAlarm.VERIFY_DONE)
                    .set(FireAlarm::getResult, FireAlarm.RESULT_FAULT)
                    .set(FireAlarm::getHandleSource, handleSource)
                    .set(FireAlarm::getHandlerId, operatorId)
                    .set(FireAlarm::getHandlerName, operatorName)
                    .set(FireAlarm::getFinalizeTime, now);
        }
        int rows = alarmMapper.update(null, update);
        if (rows == 0) {
            return 0;
        }
        DangerTrace trace = new DangerTrace();
        trace.setTenantId(alarm.getTenantId());
        trace.setDangerId(dangerId);
        trace.setTraceType(DangerTrace.TYPE_CONVERT);
        trace.setResult(1);
        trace.setContent("由故障报警转隐患（" + (CONVERT_AUTO.equals(convertSource) ? "自动转" : "人工转") + "）");
        Map<String, Object> dangerDetail = new HashMap<>(4);
        dangerDetail.put("convertSource", convertSource);
        dangerDetail.put("alarmId", alarm.getId());
        dangerDetail.put("alarmTime", alarm.getAlarmTime());
        trace.setDetail(toJson(dangerDetail));
        trace.setCreateTime(now);
        dangerTraceMapper.insert(trace);

        Map<String, Object> alarmDetail = new HashMap<>(4);
        alarmDetail.put("convertSource", convertSource);
        alarmDetail.put("dangerId", dangerId);
        appendTrace(alarm, FireAlarmTrace.TYPE_CONVERT_DANGER, 1,
                "转隐患：" + (CONVERT_AUTO.equals(convertSource) ? "自动转" : "人工转"), alarmDetail);
        return rows;
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

    private FireAlarm requireAlarm(Long alarmId) {
        FireAlarm alarm = alarmId == null ? null : alarmMapper.selectById(alarmId);
        if (alarm == null) {
            throw new BizException("报警不存在");
        }
        return alarm;
    }

    private void validateResult(Integer result) {
        if (result == null
                || (result != FireAlarm.RESULT_FALSE && result != FireAlarm.RESULT_TRUE
                && result != FireAlarm.RESULT_FAULT)) {
            throw new BizException("核警结论不合法");
        }
    }

    private LocalDateTime convertDeadline(FireAlarm alarm) {
        LocalDateTime base = alarm.getAlarmTime() == null ? LocalDateTime.now() : alarm.getAlarmTime();
        return base.plusHours(CONVERT_WINDOW_HOURS);
    }

    private String manualHandleSource() {
        try {
            if (!StpUtil.isLogin()) {
                return FireAlarm.SOURCE_DUTY;
            }
            Object roleKeys = StpUtil.getSession().get("roleKeys");
            if (roleKeys instanceof Collection
                    && ((Collection<?>) roleKeys).contains("TENANT_ADMIN")) {
                return FireAlarm.SOURCE_ADMIN;
            }
        } catch (Exception ex) {
            log.debug("resolve handle source failed", ex);
        }
        return FireAlarm.SOURCE_DUTY;
    }

    private String resultText(Integer result) {
        if (Integer.valueOf(FireAlarm.RESULT_FALSE).equals(result)) {
            return "误报";
        }
        if (Integer.valueOf(FireAlarm.RESULT_TRUE).equals(result)) {
            return "真实火警";
        }
        return "故障";
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            log.warn("serialize trace detail failed", ex);
            return null;
        }
    }

    private String safe(String value) {
        return StringUtils.hasText(value) ? value : "";
    }
}
