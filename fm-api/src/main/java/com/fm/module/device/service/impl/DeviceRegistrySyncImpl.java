package com.fm.module.device.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fm.module.device.entity.Device;
import com.fm.module.device.service.DeviceRegistrySync;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 设备注册表同步实现：写 fm:device:{sn} 哈希与 fm:device:all 集合。
 * 同时写入 aepId 字段与 fm:device:aep:{aepId} 反查键，供采集器在只有 deviceId 时找到 sn。
 * 与采集器侧 DeviceRegistry 读写同一份结构。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceRegistrySyncImpl implements DeviceRegistrySync {

    private static final String KEY_PREFIX = "fm:device:";
    private static final String KEY_ALL = "fm:device:all";
    private static final String KEY_AEP_PREFIX = "fm:device:aep:";
    private static final String FIELD_AEP_ID = "aepId";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void refresh(Device device) {
        if (device == null || !StringUtils.hasText(device.getSn())) {
            return;
        }
        String aepId = resolveAepId(device);
        Map<String, String> values = new HashMap<>(8);
        values.put("tenantId", String.valueOf(device.getTenantId() == null ? 0L : device.getTenantId()));
        values.put("deviceType", device.getType() == null ? "" : device.getType());
        values.put("online", String.valueOf(device.getOnlineStatus() == null ? 0 : device.getOnlineStatus()));
        values.put("status", String.valueOf(device.getStatus() == null ? 0 : device.getStatus()));
        if (StringUtils.hasText(aepId)) {
            values.put(FIELD_AEP_ID, aepId);
        }
        try {
            redisTemplate.opsForHash().putAll(KEY_PREFIX + device.getSn(), values);
            redisTemplate.opsForSet().add(KEY_ALL, device.getSn());
            if (StringUtils.hasText(aepId)) {
                redisTemplate.opsForValue().set(KEY_AEP_PREFIX + aepId, device.getSn());
            }
        } catch (Exception ex) {
            log.error("refresh device registry failed, sn={}", device.getSn(), ex);
        }
    }

    @Override
    public void remove(String sn) {
        if (!StringUtils.hasText(sn)) {
            return;
        }
        try {
            Object aepId = redisTemplate.opsForHash().get(KEY_PREFIX + sn, FIELD_AEP_ID);
            redisTemplate.delete(KEY_PREFIX + sn);
            redisTemplate.opsForSet().remove(KEY_ALL, sn);
            if (aepId != null && StringUtils.hasText(String.valueOf(aepId))) {
                redisTemplate.delete(KEY_AEP_PREFIX + aepId);
            }
        } catch (Exception ex) {
            log.error("remove device registry failed, sn={}", sn, ex);
        }
    }

    private String resolveAepId(Device device) {
        if (!StringUtils.hasText(device.getFExt())) {
            return null;
        }
        try {
            JsonNode aepId = objectMapper.readTree(device.getFExt()).get(FIELD_AEP_ID);
            return aepId == null || aepId.isNull() ? null : aepId.asText();
        } catch (JsonProcessingException ex) {
            log.warn("invalid f_ext when syncing device registry, sn={}", device.getSn(), ex);
            return null;
        }
    }
}
