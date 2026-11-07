package com.fm.module.alarm.job;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fm.common.config.SchedulerLock;
import com.fm.common.context.TenantContext;
import com.fm.module.alarm.entity.CommandTask;
import com.fm.module.alarm.mapper.CommandTaskMapper;
import com.fm.module.alarm.mq.MqConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 指令超时任务：SENDING/SENT 超过 60 秒仍未收到回执的置为 TIMEOUT。
 * 扫描跨租户，任务本身带 tenant_id，无需按租户拆分；多实例部署用 Redis 锁防重。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommandTimeoutJob {

    private static final long TIMEOUT_SECONDS = 60L;
    private static final long SCAN_DELAY_MILLIS = 30000L;
    private static final String LOCK_KEY = "scheduler:lock:command-timeout";
    private static final long LOCK_TTL_SECONDS = 120L;

    private final CommandTaskMapper taskMapper;
    private final SchedulerLock schedulerLock;

    @Scheduled(fixedDelay = SCAN_DELAY_MILLIS)
    public void markTimeout() {
        Optional<String> lockToken = schedulerLock.tryLock(LOCK_KEY, LOCK_TTL_SECONDS);
        if (!lockToken.isPresent()) {
            log.debug("command timeout job skipped, lock not acquired, lockKey={}", LOCK_KEY);
            return;
        }
        try {
            TenantContext.set(null, "JOB", "ALL", true);
            try {
                // SENDING 也纳入扫描：事务提交后、confirm 未回前的任务不能永久悬停
                LocalDateTime deadline = LocalDateTime.now().minusSeconds(TIMEOUT_SECONDS);
                int updated = taskMapper.update(null, new LambdaUpdateWrapper<CommandTask>()
                        .in(CommandTask::getStatus, MqConstants.COMMAND_STATUS_SENDING,
                                CommandTask.STATUS_SENT)
                        .lt(CommandTask::getReqTime, deadline)
                        .set(CommandTask::getStatus, CommandTask.STATUS_TIMEOUT)
                        .set(CommandTask::getResult, "no ack in " + TIMEOUT_SECONDS + "s")
                        .set(CommandTask::getUpdateTime, LocalDateTime.now()));
                if (updated > 0) {
                    log.warn("command tasks marked timeout: {}", updated);
                }
            } finally {
                TenantContext.clear();
            }
        } finally {
            schedulerLock.release(LOCK_KEY, lockToken.get());
        }
    }
}
