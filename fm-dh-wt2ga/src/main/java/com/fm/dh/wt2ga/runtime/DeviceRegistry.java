package com.fm.dh.wt2ga.runtime;

import com.fm.dh.wt2ga.config.RegistryProperties;
import com.fm.dh.wt2ga.contract.DeviceInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 设备注册表：从 Redis 加载 fm:device:all 与 fm:device:{sn}，构建 sn / aepId 双索引。
 *
 * <p>刷新时先构建新的索引 Map，再整体替换引用，读请求不会看到空表；
 * Redis 异常时保留上一版数据并记 WARN。按 5000 台、60 秒一轮估算，
 * 每轮 1 次 SMEMBERS + 5000 次 HGETALL，约 83 次/秒。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceRegistry {

    private static final String KEY_ALL = "fm:device:all";
    private static final String KEY_PREFIX = "fm:device:";
    private static final String FIELD_TENANT_ID = "tenantId";
    private static final String FIELD_DEVICE_TYPE = "deviceType";
    private static final String FIELD_AEP_ID = "aepId";

    private final StringRedisTemplate redisTemplate;
    private final RegistryProperties properties;

    private volatile Map<String, DeviceInfo> snIndex = Collections.emptyMap();
    private volatile Map<String, DeviceInfo> aepIndex = Collections.emptyMap();

    @PostConstruct
    public void init() {
        refresh();
    }

    @Scheduled(fixedDelayString = "${fm.registry.refresh-interval-ms}")
    public void refresh() {
        try {
            Set<String> sns = redisTemplate.opsForSet().members(KEY_ALL);
            Map<String, DeviceInfo> newSnIndex = new HashMap<>(capacity(sns));
            Map<String, DeviceInfo> newAepIndex = new HashMap<>(capacity(sns));
            if (sns != null) {
                for (String sn : sns) {
                    if (!StringUtils.hasText(sn)) {
                        continue;
                    }
                    Map<Object, Object> hash = redisTemplate.opsForHash().entries(KEY_PREFIX + sn);
                    DeviceInfo device = toDeviceInfo(sn, hash);
                    if (device == null) {
                        continue;
                    }
                    newSnIndex.put(sn, device);
                    if (StringUtils.hasText(device.getAepId())) {
                        newAepIndex.put(device.getAepId(), device);
                    }
                }
            }
            snIndex = Collections.unmodifiableMap(newSnIndex);
            aepIndex = Collections.unmodifiableMap(newAepIndex);
            log.info("device registry refreshed, sn={}, aep={}", newSnIndex.size(), newAepIndex.size());
        } catch (RuntimeException ex) {
            log.warn("device registry refresh failed, keep previous snapshot", ex);
        }
    }

    public Optional<DeviceInfo> bySn(String sn) {
        if (!StringUtils.hasText(sn)) {
            return Optional.empty();
        }
        return Optional.ofNullable(snIndex.get(sn));
    }

    public Optional<DeviceInfo> byAepId(String aepId) {
        if (!StringUtils.hasText(aepId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(aepIndex.get(aepId));
    }

    private DeviceInfo toDeviceInfo(String sn, Map<Object, Object> hash) {
        if (hash == null || hash.isEmpty()) {
            log.warn("device registry entry missing, sn={}", sn);
            return null;
        }
        String expectedType = properties.getDeviceType();
        Object deviceType = hash.get(FIELD_DEVICE_TYPE);
        if (expectedType == null || !expectedType.equals(deviceType)) {
            return null;
        }
        return DeviceInfo.builder()
                .sn(sn)
                .aepId(text(hash.get(FIELD_AEP_ID)))
                .tenantId(number(hash.get(FIELD_TENANT_ID), sn))
                .deviceType(expectedType)
                .build();
    }

    private Long number(Object value, String sn) {
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException ex) {
            log.warn("device registry tenantId invalid, sn={}, value={}", sn, value);
            return null;
        }
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private int capacity(Set<String> sns) {
        return sns == null ? 16 : Math.max(16, sns.size() * 2);
    }
}
