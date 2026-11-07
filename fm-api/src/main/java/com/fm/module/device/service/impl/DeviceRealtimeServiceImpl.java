package com.fm.module.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fm.common.constant.TimeZones;
import com.fm.common.context.TenantContext;
import com.fm.module.alarm.mq.DeviceMessage;
import com.fm.module.device.entity.Device;
import com.fm.module.device.entity.DeviceRealtime;
import com.fm.module.device.mapper.DeviceMapper;
import com.fm.module.device.mapper.DeviceRealtimeMapper;
import com.fm.module.device.service.DeviceRealtimeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * 实时快照入库实现：按 sn 唯一键覆盖写入，供设备列表与大屏展示。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceRealtimeServiceImpl implements DeviceRealtimeService {

    private final DeviceRealtimeMapper realtimeMapper;
    private final DeviceMapper deviceMapper;

    @Override
    public void save(DeviceMessage message) {
        if (message.getTenantId() == null) {
            log.warn("realtime message without tenantId dropped, messageId={}", message.getMessageId());
            return;
        }
        TenantContext.set(message.getTenantId(), "MQ", "ALL", false);
        try {
            Device device = deviceMapper.selectOne(new LambdaQueryWrapper<Device>()
                    .eq(Device::getSn, message.getSn()));
            if (device == null) {
                log.warn("realtime message from unregistered device dropped, sn={}", message.getSn());
                return;
            }
            JsonNode payload = message.getPayload();
            String data = resolveData(payload);
            LocalDateTime reportTime = resolveReportTime(payload, message.getEventTime());
            DeviceRealtime existing = realtimeMapper.selectOne(new LambdaQueryWrapper<DeviceRealtime>()
                    .eq(DeviceRealtime::getSn, message.getSn()));
            if (existing == null) {
                insert(message, device, data, reportTime);
            } else {
                update(message, data, reportTime);
            }
        } finally {
            TenantContext.clear();
        }
    }

    private void insert(DeviceMessage message, Device device, String data, LocalDateTime reportTime) {
        DeviceRealtime entity = new DeviceRealtime();
        entity.setTenantId(message.getTenantId());
        entity.setSn(message.getSn());
        entity.setType(device.getType());
        entity.setData(data);
        entity.setReportTime(reportTime);
        entity.setUpdateTime(LocalDateTime.now());
        try {
            realtimeMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            update(message, data, reportTime);
        }
    }

    private void update(DeviceMessage message, String data, LocalDateTime reportTime) {
        realtimeMapper.update(null, new LambdaUpdateWrapper<DeviceRealtime>()
                .eq(DeviceRealtime::getSn, message.getSn())
                .set(DeviceRealtime::getData, data)
                .set(DeviceRealtime::getReportTime, reportTime)
                .set(DeviceRealtime::getUpdateTime, LocalDateTime.now()));
    }

    private String resolveData(JsonNode payload) {
        if (payload == null) {
            return null;
        }
        JsonNode data = payload.get("data");
        return data == null || data.isNull() ? payload.toString() : data.toString();
    }

    private LocalDateTime resolveReportTime(JsonNode payload, Long eventTime) {
        long reportTime = payload == null ? 0L : payload.path("reportTime").asLong(0L);
        long epochMillis = reportTime > 0L ? reportTime : (eventTime == null ? System.currentTimeMillis() : eventTime);
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), TimeZones.BUSINESS);
    }
}
