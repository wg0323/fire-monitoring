package com.fm.module.alarm.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fm.common.config.SchedulerLock;
import com.fm.common.constant.TimeZones;
import com.fm.common.context.TenantContext;
import com.fm.module.alarm.entity.FireAlarm;
import com.fm.module.alarm.mapper.FireAlarmMapper;
import com.fm.module.alarm.service.AlarmDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 故障转隐患定时任务：每 5 分钟先处理 1 小时内自行恢复，再扫描到期未转的故障报警。
 * 历史报警 danger_convert_deadline 为空，不参与自动转；多实例部署用 Redis 锁防重。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DangerConvertJob {

    private static final int BATCH_SIZE = 200;
    private static final String LOCK_KEY = "scheduler:lock:danger-convert";
    private static final long LOCK_TTL_SECONDS = 600L;

    private final FireAlarmMapper alarmMapper;
    private final AlarmDomainService alarmDomainService;
    private final SchedulerLock schedulerLock;

    @Scheduled(initialDelay = 60_000L, fixedDelay = 300_000L)
    public void run() {
        Optional<String> lockToken = schedulerLock.tryLock(LOCK_KEY, LOCK_TTL_SECONDS);
        if (!lockToken.isPresent()) {
            log.debug("danger convert job skipped, lock not acquired, lockKey={}", LOCK_KEY);
            return;
        }
        try {
            TenantContext.set(0L, "SYSTEM", "ALL", true);
            try {
                recoverInWindowBatch();
                convertDueBatch();
            } catch (Exception ex) {
                log.error("danger convert job failed", ex);
            } finally {
                TenantContext.clear();
            }
        } finally {
            schedulerLock.release(LOCK_KEY, lockToken.get());
        }
    }

    /**
     * 1 小时内自行恢复：recover_time <= danger_convert_deadline，收口且不转隐患。
     */
    private void recoverInWindowBatch() {
        List<FireAlarm> alarms = alarmMapper.selectList(new LambdaQueryWrapper<FireAlarm>()
                .isNotNull(FireAlarm::getDangerConvertDeadline)
                .isNotNull(FireAlarm::getRecoverTime)
                .isNull(FireAlarm::getDangerId)
                .apply("recover_time <= danger_convert_deadline")
                .last("limit " + BATCH_SIZE));
        for (FireAlarm alarm : alarms) {
            try {
                alarmDomainService.recoverInWindow(alarm.getId());
            } catch (Exception ex) {
                log.error("recover in window failed, alarmId={}", alarm.getId(), ex);
            }
        }
    }

    /**
     * 到期未转：danger_convert_deadline <= now，且未在窗口内恢复。
     */
    private void convertDueBatch() {
        LocalDateTime now = LocalDateTime.now(TimeZones.BUSINESS);
        List<FireAlarm> alarms = alarmMapper.selectList(new LambdaQueryWrapper<FireAlarm>()
                .isNotNull(FireAlarm::getDangerConvertDeadline)
                .le(FireAlarm::getDangerConvertDeadline, now)
                .isNull(FireAlarm::getDangerId)
                .and(w -> w.isNull(FireAlarm::getRecoverTime)
                        .or().apply("recover_time > danger_convert_deadline"))
                .last("limit " + BATCH_SIZE));
        for (FireAlarm alarm : alarms) {
            if (!AlarmDomainService.shouldAutoConvert(alarm, now)) {
                continue;
            }
            try {
                alarmDomainService.convertDanger(alarm.getId(), "AUTO", null, "系统");
            } catch (Exception ex) {
                log.error("auto convert danger failed, alarmId={}", alarm.getId(), ex);
            }
        }
    }
}
