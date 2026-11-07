package com.fm.module.notification.service.impl;

import com.fm.common.context.TenantContext;
import com.fm.module.alarm.entity.AlarmLevel;
import com.fm.module.alarm.entity.FireAlarm;
import com.fm.module.alarm.event.AlarmCreatedEvent;
import com.fm.module.device.entity.Merchant;
import com.fm.module.device.mapper.MerchantMapper;
import com.fm.module.notification.service.NotifyService;
import com.fm.module.notification.service.SmsService;
import com.fm.module.notification.service.WxSubscribeService;
import com.fm.module.notification.service.MpNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executor;

/**
 * 报警通知编排实现：短信 + 小程序订阅消息，任一渠道异常只记录日志，向上不抛异常。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyServiceImpl implements NotifyService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SmsService smsService;
    private final WxSubscribeService wxSubscribeService;
    private final MpNotificationService notificationService;
    private final MerchantMapper merchantMapper;
    private final Executor notifyExecutor;

    /**
     * 报警事务提交后才推通知，避免报警回滚后仍然发出短信/订阅消息。
     * 外部 HTTP 调用不能阻塞 device.up 消费线程（prefetch=1），故交给有界线程池异步执行。
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleAlarmCreated(AlarmCreatedEvent event) {
        FireAlarm alarm = event.getAlarm();
        // 发布方在事务方法内 finally 清理了 ThreadLocal，AFTER_COMMIT 时上下文已为空；
        // 在异步任务内部按告警所属租户重建，避免商户查询被追加 tenant_id=0 而静默跳过全部通知。
        notifyExecutor.execute(() -> {
            TenantContext.set(alarm.getTenantId(), "SYSTEM", "ALL", false);
            try {
                onAlarm(alarm);
            } finally {
                TenantContext.clear();
            }
        });
    }

    @Override
    public void onAlarm(FireAlarm alarm) {
        try {
            Merchant merchant = alarm.getMerchantId() == null ? null : merchantMapper.selectById(alarm.getMerchantId());
            if (merchant == null) {
                log.warn("alarm notify skipped: merchant not found, alarmId={}", alarm.getId());
                return;
            }
            String address = StringUtils.hasText(alarm.getPosition()) ? alarm.getPosition() : merchant.getName();
            notificationService.createAlarmNotifications(alarm, merchant);
            wxSubscribeService.sendAlarm(alarm, merchant);

            String phone = StringUtils.hasText(merchant.getPhone1()) ? merchant.getPhone1() : merchant.getPhone();
            if (!StringUtils.hasText(phone)) {
                log.info("alarm sms skipped: merchant phone empty, alarmId={}", alarm.getId());
                return;
            }
            boolean sent = smsService.sendAlarm(phone, alarm.getAlarmTime().format(TIME_FORMATTER),
                    levelText(alarm.getAlarmLevel()), address);
            log.info("alarm sms result: alarmId={}, phone={}, success={}", alarm.getId(), phone, sent);
        } catch (Exception ex) {
            log.error("alarm notify failed, alarmId={}", alarm.getId(), ex);
        }
    }

    private String levelText(Integer level) {
        if (level == null) {
            return "报警";
        }
        if (level == AlarmLevel.FAULT) {
            return "故障";
        }
        return "火警";
    }
}
