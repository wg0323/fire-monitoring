package com.fm.dh.wt2ga.ctwing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fm.dh.wt2ga.contract.DeviceInfo;
import com.fm.dh.wt2ga.contract.DeviceMessage;
import com.fm.dh.wt2ga.contract.Envelope;
import com.fm.dh.wt2ga.contract.MessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实时快照：设备把温度、电池、信号拆在不同 serviceId 上报，按 sn 合并后整包下发，
 * 避免 fm-api 的实时表被最后一条局部报文覆盖。5000 台规模约 2.5 MB，不做淘汰。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimeSnapshot {

    /** 推送字段 → 快照字段，白名单外的字段一律丢弃 */
    private static final Map<String, String> FIELD_NAMES = buildFieldNames();

    private final ObjectMapper objectMapper;
    private final MessagePublisher publisher;
    private final Map<String, Map<String, Object>> snapshots = new ConcurrentHashMap<>();

    public DeviceMessage merge(CtwingPushMessage push, DeviceInfo device, String rawBody) {
        Map<String, Object> merged = snapshots.computeIfAbsent(device.getSn(), key -> new ConcurrentHashMap<>());
        collect(push.getPayload(), merged);

        ObjectNode data = objectMapper.createObjectNode();
        merged.forEach((key, value) -> data.set(key, objectMapper.valueToTree(value)));
        long reportTime = push.getTimestamp() == null ? System.currentTimeMillis() : push.getTimestamp();
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("deviceType", device.getDeviceType());
        payload.set("data", data);
        payload.put("reportTime", reportTime);

        DeviceMessage message = Envelope.of(device, PushConstants.TYPE_REALTIME, reportTime, payload,
                push.getServiceId(), rawBody);
        publisher.publishRealtime(message);
        log.debug("realtime snapshot merged, sn={}, service={}, fields={}",
                device.getSn(), push.getServiceId(), data.size());
        return message;
    }

    private void collect(JsonNode source, Map<String, Object> target) {
        if (source == null || !source.isObject()) {
            return;
        }
        FIELD_NAMES.forEach((sourceKey, targetKey) -> {
            JsonNode value = source.get(sourceKey);
            if (value != null && !value.isNull()) {
                target.put(targetKey, valueOf(value));
            }
        });
    }

    private Object valueOf(JsonNode node) {
        if (node.isNumber()) {
            return node.numberValue();
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        return node.asText();
    }

    private static Map<String, String> buildFieldNames() {
        Map<String, String> names = new LinkedHashMap<>();
        names.put("temperature_value", "temperature");
        names.put("temperature_state", "temperatureState");
        names.put("battery_value", "batteryValue");
        names.put("battery_voltage", "batteryVoltage");
        names.put("signalPower", "signalPower");
        names.put("operation", "operation");
        names.put("fault", "fault");
        names.put("tamper_alarm", "tamperAlarm");
        names.put("report_count", "reportCount");
        names.put("cell_id", "cellId");
        return Collections.unmodifiableMap(names);
    }
}
