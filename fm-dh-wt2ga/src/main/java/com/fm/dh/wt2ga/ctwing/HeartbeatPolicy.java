package com.fm.dh.wt2ga.ctwing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fm.dh.wt2ga.contract.DeviceInfo;
import com.fm.dh.wt2ga.contract.DeviceMessage;
import com.fm.dh.wt2ga.contract.Envelope;
import com.fm.dh.wt2ga.contract.MessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * 上下线通知策略：设备是「上线 → 参数上报 → 两秒后下线」的周期唤醒，
 * 把下线直译成心跳会让在线状态反复抖动，因此只发上线心跳，离线交给 last_time 扫描。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HeartbeatPolicy {

    private final ObjectMapper objectMapper;
    private final MessagePublisher publisher;

    public Optional<DeviceMessage> map(CtwingPushMessage push, DeviceInfo device, String rawBody) {
        if (push == null || device == null) {
            return Optional.empty();
        }
        Integer eventType = push.getEventType();
        if (eventType == null) {
            log.warn("online/offline report without eventType dropped, sn={}", device.getSn());
            return Optional.empty();
        }
        if (eventType == PushConstants.EVENT_TYPE_OFFLINE) {
            log.info("device offline reported, online status left to last_time scan, sn={}", device.getSn());
            return Optional.empty();
        }
        if (eventType != PushConstants.EVENT_TYPE_ONLINE) {
            log.warn("unexpected online/offline eventType dropped, sn={}, eventType={}", device.getSn(), eventType);
            return Optional.empty();
        }
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("online", true);
        if (StringUtils.hasText(push.getIpv4Address())) {
            payload.put("ip", push.getIpv4Address());
        }
        long eventTime = push.getTimestamp() == null ? System.currentTimeMillis() : push.getTimestamp();
        DeviceMessage message = Envelope.of(device, PushConstants.TYPE_HEARTBEAT, eventTime, payload,
                push.getServiceId(), rawBody);
        publisher.publishUp(message);
        return Optional.of(message);
    }
}
