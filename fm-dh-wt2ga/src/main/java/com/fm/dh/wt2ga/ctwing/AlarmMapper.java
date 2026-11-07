package com.fm.dh.wt2ga.ctwing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fm.dh.wt2ga.contract.DeviceInfo;
import com.fm.dh.wt2ga.contract.DeviceMessage;
import com.fm.dh.wt2ga.contract.Envelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * eventReport 映射：温度报警、设备拆装、故障码三类事件统一转成 ALARM / RECOVER 信封。
 * 报警与恢复都发，恢复不再依赖人工核警收口。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlarmMapper {

    private final ObjectMapper objectMapper;
    private final ThingModel thingModel;

    public Optional<DeviceMessage> map(CtwingPushMessage push, DeviceInfo device, String rawBody) {
        if (push == null || device == null
                || !PushConstants.MESSAGE_TYPE_EVENT_REPORT.equals(push.getMessageType())) {
            return Optional.empty();
        }
        JsonNode content = push.getEventContent();
        if (content == null || content.isNull()) {
            log.warn("eventReport without eventContent dropped, sn={}, serviceId={}",
                    device.getSn(), push.getServiceId());
            return Optional.empty();
        }
        if (matches(push.getServiceId(), PushConstants.SERVICE_TEMPERATURE_STATE,
                PushConstants.SERVICE_CODE_TEMPERATURE_STATE)) {
            return mapTemperatureState(push, device, content, rawBody);
        }
        if (matches(push.getServiceId(), PushConstants.SERVICE_REMOVE_REPORT,
                PushConstants.SERVICE_CODE_REMOVE_REPORT)) {
            return mapRemoveReport(push, device, content, rawBody);
        }
        if (matches(push.getServiceId(), PushConstants.SERVICE_FAULT_CODE_REPORT,
                PushConstants.SERVICE_CODE_FAULT_CODE_REPORT)) {
            return mapFaultCode(push, device, content, rawBody);
        }
        log.debug("eventReport service ignored, sn={}, serviceId={}", device.getSn(), push.getServiceId());
        return Optional.empty();
    }

    private Optional<DeviceMessage> mapTemperatureState(CtwingPushMessage push, DeviceInfo device,
                                                        JsonNode content, String rawBody) {
        int state = content.path("temperature_state").asInt(-1);
        if (state == PushConstants.TEMPERATURE_STATE_ALARM) {
            ObjectNode data = objectMapper.createObjectNode()
                    .put("temperatureState", state);
            data.set("temperature", content.get("temperature_value"));
            return Optional.of(build(push, device, rawBody, PushConstants.TYPE_ALARM, PushConstants.ALARM_TYPE_FIRE,
                    PushConstants.LEVEL_FIRE, PushConstants.EVENT_CODE_TEMPERATURE, data,
                    PushConstants.ENUM_TEMPERATURE_STATE, state));
        }
        if (state == PushConstants.TEMPERATURE_STATE_NORMAL) {
            ObjectNode data = objectMapper.createObjectNode();
            data.set("temperature", content.get("temperature_value"));
            return Optional.of(build(push, device, rawBody, PushConstants.TYPE_RECOVER,
                    PushConstants.ALARM_TYPE_RECOVER, null, PushConstants.EVENT_CODE_TEMPERATURE, data,
                    PushConstants.ENUM_TEMPERATURE_STATE, state));
        }
        log.warn("unexpected temperature_state dropped, sn={}, state={}", device.getSn(), state);
        return Optional.empty();
    }

    private Optional<DeviceMessage> mapRemoveReport(CtwingPushMessage push, DeviceInfo device,
                                                    JsonNode content, String rawBody) {
        int tamperAlarm = content.path("tamper_alarm").asInt(-1);
        if (tamperAlarm == PushConstants.TAMPER_ALARM_ON) {
            ObjectNode data = objectMapper.createObjectNode().put("tamperAlarm", tamperAlarm);
            return Optional.of(build(push, device, rawBody, PushConstants.TYPE_ALARM, PushConstants.ALARM_TYPE_FIRE,
                    PushConstants.LEVEL_FIRE, PushConstants.EVENT_CODE_REMOVE, data,
                    PushConstants.ENUM_TAMPER_ALARM, tamperAlarm));
        }
        if (tamperAlarm == PushConstants.TAMPER_ALARM_OFF) {
            return Optional.of(build(push, device, rawBody, PushConstants.TYPE_RECOVER,
                    PushConstants.ALARM_TYPE_RECOVER, null, PushConstants.EVENT_CODE_REMOVE, null,
                    PushConstants.ENUM_TAMPER_ALARM, tamperAlarm));
        }
        log.warn("unexpected tamper_alarm dropped, sn={}, value={}", device.getSn(), tamperAlarm);
        return Optional.empty();
    }

    private Optional<DeviceMessage> mapFaultCode(CtwingPushMessage push, DeviceInfo device,
                                                 JsonNode content, String rawBody) {
        int fault = content.path("fault").asInt(-1);
        if (fault == PushConstants.FAULT_TEMPERATURE_SENSOR || fault == PushConstants.FAULT_LOW_VOLTAGE) {
            ObjectNode data = objectMapper.createObjectNode().put("fault", fault);
            return Optional.of(build(push, device, rawBody, PushConstants.TYPE_ALARM, PushConstants.ALARM_TYPE_FAULT,
                    PushConstants.LEVEL_FAULT, PushConstants.EVENT_CODE_FAULT, data, PushConstants.ENUM_FAULT, fault));
        }
        if (fault == PushConstants.FAULT_LOW_VOLTAGE_RECOVERED) {
            ObjectNode data = objectMapper.createObjectNode().put("fault", fault);
            return Optional.of(build(push, device, rawBody, PushConstants.TYPE_RECOVER,
                    PushConstants.ALARM_TYPE_RECOVER, null, PushConstants.EVENT_CODE_FAULT, data,
                    PushConstants.ENUM_FAULT, fault));
        }
        log.warn("unexpected fault code dropped, sn={}, fault={}", device.getSn(), fault);
        return Optional.empty();
    }

    private DeviceMessage build(CtwingPushMessage push, DeviceInfo device, String rawBody, String type,
                                String alarmType, Integer level, int eventCode, ObjectNode data,
                                String enumKey, Object enumValue) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("alarmType", alarmType);
        if (level != null) {
            payload.put("level", level);
        }
        payload.put("eventCode", eventCode);
        if (data != null) {
            payload.set("data", data);
        }
        String remark = thingModel.enumText(enumKey, enumValue);
        if (remark != null) {
            payload.put("remark", remark);
        }
        long eventTime = push.getTimestamp() == null ? System.currentTimeMillis() : push.getTimestamp();
        log.info("eventReport mapped, sn={}, service={}, type={}",
                device.getSn(), thingModel.serviceName(push.getServiceId()), type);
        return Envelope.of(device, type, eventTime, payload, push.getServiceId(), rawBody);
    }

    private boolean matches(String serviceId, String identifier, int code) {
        return identifier.equals(serviceId) || String.valueOf(code).equals(serviceId);
    }
}
