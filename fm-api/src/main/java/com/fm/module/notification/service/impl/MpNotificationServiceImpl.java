package com.fm.module.notification.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fm.common.exception.BizException;
import com.fm.module.alarm.entity.AlarmLevel;
import com.fm.module.alarm.entity.FireAlarm;
import com.fm.module.danger.entity.Danger;
import com.fm.module.device.entity.Merchant;
import com.fm.module.notification.converter.NotificationConverter;
import com.fm.module.notification.dto.NotificationQueryDTO;
import com.fm.module.notification.entity.MpNotification;
import com.fm.module.notification.mapper.MpNotificationMapper;
import com.fm.module.notification.service.MpNotificationService;
import com.fm.module.notification.vo.MpNotificationVO;
import com.fm.module.notification.vo.SubscribeConfigVO;
import com.fm.module.notification.service.WxSubscribeService;
import com.fm.module.system.entity.SysUser;
import com.fm.module.system.mapper.SysUserMapper;
import com.fm.module.system.service.DataScopeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 站内通知实现：报警发给商户账号，隐患发给运营账号。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MpNotificationServiceImpl implements MpNotificationService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String SUBSCRIBE_KEY_PREFIX = "fm:mp:subscribe:";
    private static final long SUBSCRIBE_TTL_DAYS = 30L;

    private final MpNotificationMapper notificationMapper;
    private final SysUserMapper userMapper;
    private final DataScopeService dataScopeService;
    private final WxSubscribeService wxSubscribeService;
    private final StringRedisTemplate redisTemplate;

    @Override
    public void createAlarmNotifications(FireAlarm alarm, Merchant merchant) {
        try {
            List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getTenantId, alarm.getTenantId())
                    .eq(SysUser::getUserType, "TENANT")
                    .eq(SysUser::getStatus, 1));
            int type = alarm.getAlarmLevel() != null && alarm.getAlarmLevel() == AlarmLevel.FIRE
                    ? MpNotification.TYPE_ALARM : MpNotification.TYPE_FAULT;
            String position = StringUtils.hasText(alarm.getPosition()) ? alarm.getPosition() : merchant.getName();
            String content = "报警通知：" + levelText(alarm.getAlarmLevel()) + " " + position
                    + "，时间 " + (alarm.getAlarmTime() == null ? "-" : alarm.getAlarmTime().format(TIME_FORMATTER));
            for (SysUser user : users) {
                insert(alarm.getTenantId(), user, type, content, alarm.getId(), "inapp", 1, null);
            }
        } catch (Exception ex) {
            log.error("create alarm notifications failed, alarmId={}", alarm.getId(), ex);
        }
    }

    @Override
    public void sendDanger(Danger danger, Collection<SysUser> receivers, String content) {
        if (danger == null || receivers == null || receivers.isEmpty()) {
            return;
        }
        Set<Long> sent = new HashSet<>(receivers.size());
        for (SysUser user : receivers) {
            if (user == null || user.getId() == null || !sent.add(user.getId())) {
                continue;
            }
            try {
                insert(danger.getTenantId(), user, MpNotification.TYPE_DANGER, content,
                        danger.getId(), "inapp", 1, null);
            } catch (Exception ex) {
                log.error("create danger notification failed, dangerId={}, userId={}",
                        danger.getId(), user.getId(), ex);
            }
            sendSubscribeAfterCommit(danger, user, content);
        }
    }

    /**
     * 订阅消息在事务提交后发送，避免微信接口耗时占用业务事务；无事务时直接发送。
     */
    private void sendSubscribeAfterCommit(Danger danger, SysUser user, String content) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            wxSubscribeService.sendDanger(danger, user, content);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                wxSubscribeService.sendDanger(danger, user, content);
            }
        });
    }

    @Override
    public SubscribeConfigVO subscribeConfig() {
        return NotificationConverter.subscribeConfig(wxSubscribeService.alarmTemplateId(),
                wxSubscribeService.dangerTemplateId());
    }

    @Override
    public void recordSubscribe(List<String> templateIds) {
        Long userId = StpUtil.getLoginIdAsLong();
        String value = String.join(",", templateIds) + "|" + LocalDateTime.now();
        redisTemplate.opsForValue().set(SUBSCRIBE_KEY_PREFIX + userId, value,
                SUBSCRIBE_TTL_DAYS, TimeUnit.DAYS);
    }

    @Override
    public Page<MpNotificationVO> myPage(long pageNum, long pageSize, Integer type) {
        Long userId = StpUtil.getLoginIdAsLong();
        LambdaQueryWrapper<MpNotification> wrapper = new LambdaQueryWrapper<MpNotification>()
                .eq(MpNotification::getUserId, userId)
                .eq(type != null, MpNotification::getType, type)
                .orderByDesc(MpNotification::getMessageTime);
        Page<MpNotification> page = notificationMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        Page<MpNotificationVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(NotificationConverter::toVO).collect(Collectors.toList()));
        return result;
    }

    @Override
    public long unreadCount() {
        Long userId = StpUtil.getLoginIdAsLong();
        Long count = notificationMapper.selectCount(new LambdaQueryWrapper<MpNotification>()
                .eq(MpNotification::getUserId, userId)
                .eq(MpNotification::getStatus, MpNotification.STATUS_UNREAD));
        return count == null ? 0L : count;
    }

    @Override
    @Transactional
    public void markRead(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        int rows = notificationMapper.update(null, new LambdaUpdateWrapper<MpNotification>()
                .eq(MpNotification::getId, id)
                .eq(MpNotification::getUserId, userId)
                .set(MpNotification::getStatus, MpNotification.STATUS_READ));
        if (rows == 0) {
            throw new BizException("消息不存在或无权操作");
        }
    }

    @Override
    public Page<MpNotificationVO> adminPage(NotificationQueryDTO query) {
        LambdaQueryWrapper<MpNotification> wrapper = new LambdaQueryWrapper<MpNotification>()
                .eq(query.getType() != null, MpNotification::getType, query.getType())
                .eq(query.getStatus() != null, MpNotification::getStatus, query.getStatus())
                .like(StringUtils.hasText(query.getKeyword()), MpNotification::getContent, query.getKeyword())
                .orderByDesc(MpNotification::getMessageTime);
        Page<MpNotification> page;
        if (dataScopeService.isPlatform()) {
            List<Long> tenantIds = dataScopeService.authorizedTenantIds();
            if (tenantIds.isEmpty()) {
                return new Page<>(query.getPageNum(), query.getPageSize());
            }
            wrapper.in(MpNotification::getTenantId, tenantIds);
            page = dataScopeService.withIgnoreTenant(() -> notificationMapper.selectPage(
                    new Page<>(query.getPageNum(), query.getPageSize()), wrapper));
        } else {
            page = notificationMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        }
        Page<MpNotificationVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(toVOList(page.getRecords()));
        return result;
    }

    private void insert(Long tenantId, SysUser user, int type, String content, Long relatedId,
                        String channel, int sendStatus, String failReason) {
        MpNotification notification = new MpNotification();
        notification.setTenantId(tenantId);
        notification.setUserId(user.getId());
        notification.setUserType(user.getUserType());
        notification.setType(type);
        notification.setContent(truncate(content, 500));
        notification.setRelatedId(relatedId);
        notification.setStatus(MpNotification.STATUS_UNREAD);
        notification.setMessageTime(LocalDateTime.now());
        notification.setChannel(channel);
        notification.setSendStatus(sendStatus);
        notification.setFailReason(failReason);
        notificationMapper.insert(notification);
    }

    private List<MpNotificationVO> toVOList(List<MpNotification> list) {
        if (list.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> userIds = list.stream().map(MpNotification::getUserId)
                .distinct().collect(Collectors.toList());
        Map<Long, SysUser> users = dataScopeService.isPlatform()
                ? dataScopeService.withIgnoreTenant(() -> userMapper.selectBatchIds(userIds))
                .stream().collect(Collectors.toMap(SysUser::getId, user -> user, (a, b) -> a))
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, user -> user, (a, b) -> a));
        return list.stream().map(item -> {
            MpNotificationVO vo = NotificationConverter.toVO(item);
            SysUser user = users.get(item.getUserId());
            if (user != null) {
                vo.setUserName(user.getUserName());
                vo.setRealName(user.getRealName());
            }
            return vo;
        }).collect(Collectors.toList());
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

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
