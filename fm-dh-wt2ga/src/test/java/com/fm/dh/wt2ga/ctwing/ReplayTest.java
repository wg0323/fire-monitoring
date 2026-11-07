package com.fm.dh.wt2ga.ctwing;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fm.dh.wt2ga.config.RegistryProperties;
import com.fm.dh.wt2ga.contract.DeviceInfo;
import com.fm.dh.wt2ga.contract.DeviceMessage;
import com.fm.dh.wt2ga.contract.MessagePublisher;
import com.fm.dh.wt2ga.runtime.DeviceRegistry;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 报文回放：fixtures 全部取自线上日志与订阅说明，按真实报文逐字段校验信封与路由。
 * 同一份报文连发两次，messageId 必须不变，平台重推才能被 fm-api 的唯一索引吞掉。
 */
class ReplayTest {

    private static final String ROUTE_UP = "up";
    private static final String ROUTE_REALTIME = "realtime";

    private final ObjectMapper objectMapper = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private final MessagePublisher publisher = mock(MessagePublisher.class);
    private PushDispatcher dispatcher;

    static Stream<Arguments> fixtures() {
        return Stream.of(
                Arguments.of("temperature_alarm.json", "ALARM", ROUTE_UP, "864852062800536", "FIRE", 1004, null),
                Arguments.of("temperature_recover.json", "RECOVER", ROUTE_UP, "864852062800536", "RECOVER", 1004, null),
                Arguments.of("remove_alarm.json", "ALARM", ROUTE_UP, "864852062719199", "FIRE", 1005, null),
                Arguments.of("remove_recover.json", "RECOVER", ROUTE_UP, "864852062719199", "RECOVER", 1005, null),
                Arguments.of("fault_alarm.json", "ALARM", ROUTE_UP, "864852062719199", "FAULT", 1008, null),
                Arguments.of("fault_recover.json", "RECOVER", ROUTE_UP, "864852062719199", "RECOVER", 1008, null),
                Arguments.of("online.json", "HEARTBEAT", ROUTE_UP, "864852062800536", null, null, null),
                Arguments.of("offline.json", null, null, "864852062800536", null, null, null),
                Arguments.of("devicepara.json", "REALTIME", ROUTE_REALTIME, "864852062697114", null, null,
                        "batteryValue"),
                Arguments.of("temperature_report.json", "REALTIME", ROUTE_REALTIME, "864852062800536", null, null,
                        "temperature")
        );
    }

    @ParameterizedTest
    @MethodSource("fixtures")
    void replay_produces_expected_envelope(String file, String expectedType, String expectedRoute,
                                           String expectedSn, String expectedAlarmType,
                                           Integer expectedEventCode, String expectedDataField) throws Exception {
        dispatcher(expectedSn);
        String raw = read(file);

        dispatcher.dispatch(raw);
        dispatcher.dispatch(raw);

        if (expectedType == null) {
            verify(publisher, never()).publishUp(any(DeviceMessage.class));
            verify(publisher, never()).publishRealtime(any(DeviceMessage.class));
            return;
        }
        ArgumentCaptor<DeviceMessage> captor = ArgumentCaptor.forClass(DeviceMessage.class);
        if (ROUTE_REALTIME.equals(expectedRoute)) {
            verify(publisher, times(2)).publishRealtime(captor.capture());
        } else {
            verify(publisher, times(2)).publishUp(captor.capture());
        }
        List<DeviceMessage> messages = captor.getAllValues();
        DeviceMessage first = messages.get(0);

        assertEquals(expectedType, first.getType());
        assertEquals(expectedSn, first.getSn());
        assertEquals(Long.valueOf(1L), first.getTenantId());
        assertNotNull(first.getMessageId());
        assertEquals(first.getMessageId(), messages.get(1).getMessageId());
        if (expectedAlarmType != null) {
            assertEquals(expectedAlarmType, first.getPayload().path("alarmType").asText());
            assertEquals(expectedEventCode.intValue(), first.getPayload().path("eventCode").asInt());
        }
        if (expectedDataField != null) {
            assertTrue(first.getPayload().path("data").has(expectedDataField));
        }
        if ("HEARTBEAT".equals(expectedType)) {
            assertTrue(first.getPayload().path("online").asBoolean());
        }
    }

    private void dispatcher(String expectedSn) {
        DeviceRegistry registry = mock(DeviceRegistry.class);
        DeviceInfo device = DeviceInfo.builder()
                .sn(expectedSn)
                .tenantId(1L)
                .deviceType("dh-wt2ga")
                .build();
        when(registry.bySn(anyString())).thenReturn(Optional.of(device));
        when(registry.byAepId(anyString())).thenReturn(Optional.of(device));

        RegistryProperties properties = new RegistryProperties();
        properties.setDeviceType("dh-wt2ga");
        SnResolver resolver = new SnResolver(registry, properties);
        dispatcher = new PushDispatcher(objectMapper, resolver,
                new AlarmMapper(objectMapper, new ThingModel(objectMapper)),
                new RealtimeSnapshot(objectMapper, publisher),
                new HeartbeatPolicy(objectMapper, publisher),
                publisher);
    }

    private String read(String file) throws Exception {
        try (InputStream input = new ClassPathResource("fixtures/" + file).getInputStream()) {
            return StreamUtils.copyToString(input, StandardCharsets.UTF_8).trim();
        }
    }
}
