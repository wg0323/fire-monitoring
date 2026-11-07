package com.fm.module.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fm.common.constant.TimeZones;
import com.fm.common.context.TenantContext;
import com.fm.module.alarm.mq.DeviceMessage;
import com.fm.module.device.entity.Device;
import com.fm.module.device.mapper.DeviceMapper;
import com.fm.module.device.service.DeviceRegistrySync;
import com.fm.module.device.service.DeviceStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * 心跳入库实现：更新 b_device.online_status / last_time，并刷新 Redis 注册表。
 * 离线判定由定时任务扫描 last_time 完成，不依赖心跳消息。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceStatusServiceImpl implements DeviceStatusService {

    private final DeviceMapper deviceMapper;
    private final DeviceRegistrySync registrySync;

    @Override
    public void handleHeartbeat(DeviceMessage message) {
        if (message.getTenantId() == null) {
            log.warn("heartbeat message without tenantId dropped, messageId={}", message.getMessageId());
            return;
        }
        TenantContext.set(message.getTenantId(), "MQ", "ALL", false);
        try {
            Device device = deviceMapper.selectOne(new LambdaQueryWrapper<Device>()
                    .eq(Device::getSn, message.getSn()));
            if (device == null) {
                log.warn("heartbeat from unregistered device dropped, sn={}", message.getSn());
                return;
            }
            boolean online = resolveOnline(message.getPayload());
            LocalDateTime lastTime = resolveTime(message.getEventTime());
            // 乱序心跳不得把 last_time 回退：仅当新时间更晚，或首次上报 last_time 为空时才更新
            deviceMapper.update(null, new LambdaUpdateWrapper<Device>()
                    .eq(Device::getId, device.getId())
                    .and(wrapper -> wrapper.isNull(Device::getLastTime)
                            .or().lt(Device::getLastTime, lastTime))
                    .set(Device::getOnlineStatus, online ? 1 : 0)
                    .set(Device::getLastTime, lastTime));
            device.setOnlineStatus(online ? 1 : 0);
            device.setLastTime(lastTime);
            registrySync.refresh(device);
        } finally {
            TenantContext.clear();
        }
    }

    private boolean resolveOnline(JsonNode payload) {
        return payload == null || payload.path("online").asBoolean(true);
    }

    private LocalDateTime resolveTime(Long eventTime) {
        long epochMillis = eventTime == null ? System.currentTimeMillis() : eventTime;
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), TimeZones.BUSINESS);
    }
}
