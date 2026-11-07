package com.fm.dh.wt2ga.ctwing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fm.dh.wt2ga.contract.DeviceInfo;
import com.fm.dh.wt2ga.contract.MessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 推送分发：解析报文 → 解析注册设备 → 按 messageType 分流。
 * 报文畸形或单个任务异常都只记 ERROR，不向线程池抛出。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PushDispatcher {

    private final ObjectMapper objectMapper;
    private final SnResolver snResolver;
    private final AlarmMapper alarmMapper;
    private final RealtimeSnapshot realtimeSnapshot;
    private final HeartbeatPolicy heartbeatPolicy;
    private final MessagePublisher publisher;

    public void dispatch(String rawBody) {
        try {
            CtwingPushMessage push = objectMapper.readValue(rawBody, CtwingPushMessage.class);
            Optional<DeviceInfo> device = snResolver.resolve(push);
            if (!device.isPresent()) {
                return;
            }
            dispatch(push, device.get(), rawBody);
        } catch (JsonProcessingException ex) {
            log.error("malformed ctwing push dropped, rawBody={}", rawBody, ex);
        } catch (RuntimeException ex) {
            log.error("ctwing push dispatch failed, rawBody={}", rawBody, ex);
        }
    }

    private void dispatch(CtwingPushMessage push, DeviceInfo device, String rawBody) {
        if (PushConstants.MESSAGE_TYPE_EVENT_REPORT.equals(push.getMessageType())) {
            alarmMapper.map(push, device, rawBody).ifPresent(publisher::publishUp);
            return;
        }
        if (PushConstants.MESSAGE_TYPE_DATA_REPORT.equals(push.getMessageType())) {
            realtimeSnapshot.merge(push, device, rawBody);
            return;
        }
        if (PushConstants.MESSAGE_TYPE_ONLINE_OFFLINE.equals(push.getMessageType())) {
            heartbeatPolicy.map(push, device, rawBody);
            return;
        }
        log.debug("ctwing push ignored, messageType={}, sn={}", push.getMessageType(), device.getSn());
    }
}
