package com.fm.dh.wt2ga.ctwing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fm.dh.wt2ga.contract.DeviceInfo;
import com.fm.dh.wt2ga.contract.DeviceMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * eventReport 到 ALARM / RECOVER 信封的映射测试，行数与设计文档 5.2 的事件表一一对应。
 */
class AlarmMapperTest {

    private static final String SN = "864852062800536";
    private static final String AEP_ID = "99014977864852062800536";
    private static final long TIMESTAMP = 1791138556229L;
    private static final String RAW = "{\"messageType\":\"eventReport\"}";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AlarmMapper mapper;
    private DeviceInfo device;

    @BeforeEach
    void setUp() {
        mapper = new AlarmMapper(objectMapper, new ThingModel(objectMapper));
        device = DeviceInfo.builder().sn(SN).aepId(AEP_ID).tenantId(1L).deviceType("dh-wt2ga").build();
    }

    static Stream<Arguments> mappingCases() {
        return Stream.of(
                Arguments.of("temperature_state", "{\"temperature_value\":40.6,\"temperature_state\":1}",
                        "ALARM", "FIRE", 1, 1004, "temperature"),
                Arguments.of("temperature_state", "{\"temperature_value\":48.4,\"temperature_state\":0}",
                        "RECOVER", "RECOVER", null, 1004, "temperature"),
                Arguments.of("remove_report", "{\"tamper_alarm\":1}",
                        "ALARM", "FIRE", 1, 1005, "tamperAlarm"),
                Arguments.of("remove_report", "{\"tamper_alarm\":0}",
                        "RECOVER", "RECOVER", null, 1005, null),
                Arguments.of("fault_code_report", "{\"fault\":5}",
                        "ALARM", "FAULT", 3, 1008, "fault"),
                Arguments.of("fault_code_report", "{\"fault\":7}",
                        "ALARM", "FAULT", 3, 1008, "fault"),
                Arguments.of("fault_code_report", "{\"fault\":8}",
                        "RECOVER", "RECOVER", null, 1008, "fault")
        );
    }

    @ParameterizedTest
    @MethodSource("mappingCases")
    void maps_event_report_by_service_and_state(String serviceId, String content, String expectedType,
                                                String expectedAlarmType, Integer expectedLevel,
                                                int expectedEventCode, String expectedDataKey) throws Exception {
        DeviceMessage message = mapper.map(push(serviceId, content), device, RAW)
                .orElseThrow(IllegalStateException::new);

        assertEquals(expectedType, message.getType());
        assertEquals(expectedAlarmType, message.getPayload().path("alarmType").asText());
        if (expectedLevel == null) {
            assertTrue(message.getPayload().path("level").isMissingNode());
        } else {
            assertEquals(expectedLevel.intValue(), message.getPayload().path("level").asInt());
        }
        assertEquals(expectedEventCode, message.getPayload().path("eventCode").asInt());
        if (expectedDataKey != null) {
            assertFalse(message.getPayload().path("data").path(expectedDataKey).isMissingNode());
        }
        assertEquals(TIMESTAMP, message.getEventTime().longValue());
        assertEquals(SN, message.getSn());
        assertEquals(Long.valueOf(1L), message.getTenantId());
    }

    @Test
    void temperature_alarm_keeps_raw_measurement() throws Exception {
        DeviceMessage message = mapper.map(
                push("temperature_state", "{\"temperature_value\":40.599998474121094,\"temperature_state\":1}"),
                device, RAW).orElseThrow(IllegalStateException::new);

        assertEquals(40.599998474121094, message.getPayload().path("data").path("temperature").asDouble());
        assertEquals(1, message.getPayload().path("data").path("temperatureState").asInt());
        assertEquals("温度异常告警", message.getPayload().path("remark").asText());
    }

    @Test
    void same_push_maps_to_same_message_id() throws Exception {
        String content = "{\"temperature_value\":40.6,\"temperature_state\":1}";

        String first = mapper.map(push("temperature_state", content), device, RAW)
                .orElseThrow(IllegalStateException::new).getMessageId();
        String second = mapper.map(push("temperature_state", content), device, RAW)
                .orElseThrow(IllegalStateException::new).getMessageId();

        assertEquals(first, second);
    }

    @Test
    void unknown_service_is_ignored() throws Exception {
        assertFalse(mapper.map(push("register_report", "{}"), device, RAW).isPresent());
    }

    @Test
    void non_event_report_is_ignored() throws Exception {
        CtwingPushMessage push = push("temperature_state", "{\"temperature_state\":1}");
        push.setMessageType("dataReport");

        assertFalse(mapper.map(push, device, RAW).isPresent());
    }

    @Test
    void numeric_service_id_is_accepted() throws Exception {
        Optional<DeviceMessage> message = mapper.map(push("1004", "{\"temperature_state\":1}"), device, RAW);

        assertTrue(message.isPresent());
        assertEquals("ALARM", message.get().getType());
    }

    private CtwingPushMessage push(String serviceId, String content) throws Exception {
        CtwingPushMessage push = new CtwingPushMessage();
        push.setTimestamp(TIMESTAMP);
        push.setMessageType("eventReport");
        push.setServiceId(serviceId);
        push.setEventContent(objectMapper.readTree(content));
        push.setDeviceSn(SN);
        push.setDeviceId(AEP_ID);
        return push;
    }
}
