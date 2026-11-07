package com.fm.dh.wt2ga.ctwing;

import com.fm.dh.wt2ga.config.RegistryProperties;
import com.fm.dh.wt2ga.contract.DeviceInfo;
import com.fm.dh.wt2ga.runtime.DeviceRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * sn 解析：优先用 eventReport 的 deviceSn，缺失或未注册时按 deviceId 反查 aepId 索引。
 * 两条路都查不到时记 WARN 并丢弃，日志保留平台侧原始标识便于排障。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SnResolver {

    private final DeviceRegistry deviceRegistry;
    private final RegistryProperties properties;

    public Optional<DeviceInfo> resolve(CtwingPushMessage push) {
        if (push == null) {
            return Optional.empty();
        }
        Optional<DeviceInfo> bySn = deviceRegistry.bySn(push.getDeviceSn()).filter(this::matchesType);
        if (bySn.isPresent()) {
            return bySn;
        }
        Optional<DeviceInfo> byAepId = deviceRegistry.byAepId(push.getDeviceId()).filter(this::matchesType);
        if (byAepId.isPresent()) {
            return byAepId;
        }
        log.warn("unregistered device, deviceSn={}, deviceId={}", push.getDeviceSn(), push.getDeviceId());
        return Optional.empty();
    }

    private boolean matchesType(DeviceInfo device) {
        return StringUtils.hasText(properties.getDeviceType())
                && properties.getDeviceType().equals(device.getDeviceType());
    }
}
