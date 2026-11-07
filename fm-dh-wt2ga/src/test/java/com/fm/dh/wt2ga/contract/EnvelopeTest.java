package com.fm.dh.wt2ga.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 确定性信封测试：同一份推送必须得到同一个 messageId，不同时间戳必须不同。
 */
class EnvelopeTest {

    private static final String DEVICE_ID = "D1";
    private static final String MESSAGE_TYPE = "eventReport";
    private static final String SERVICE_ID = "temperature_state";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void same_push_yields_same_message_id() {
        String first = Envelope.deterministicId(DEVICE_ID, MESSAGE_TYPE, SERVICE_ID, 1791138556229L, "{\"x\":1}");
        String second = Envelope.deterministicId(DEVICE_ID, MESSAGE_TYPE, SERVICE_ID, 1791138556229L, "{\"x\":1}");

        assertEquals(first, second);
    }

    @Test
    void different_timestamp_yields_different_message_id() {
        String first = Envelope.deterministicId(DEVICE_ID, MESSAGE_TYPE, SERVICE_ID, 1L, "{}");
        String second = Envelope.deterministicId(DEVICE_ID, MESSAGE_TYPE, SERVICE_ID, 2L, "{}");

        assertNotEquals(first, second);
    }

    @Test
    void null_raw_body_is_treated_as_empty() {
        assertEquals(Envelope.deterministicId(DEVICE_ID, MESSAGE_TYPE, SERVICE_ID, 1L, null),
                Envelope.deterministicId(DEVICE_ID, MESSAGE_TYPE, SERVICE_ID, 1L, ""));
    }

    @Test
    void envelope_carries_registry_identity_and_version() {
        DeviceInfo device = DeviceInfo.builder().sn("SN1").aepId("AEP1").tenantId(1L).deviceType("dh-wt2ga").build();
        ObjectNode payload = objectMapper.createObjectNode().put("eventCode", 1004);

        DeviceMessage message = Envelope.of(device, "ALARM", 1791138556229L, payload);

        assertEquals("SN1", message.getSn());
        assertEquals(Long.valueOf(1L), message.getTenantId());
        assertEquals("ALARM", message.getType());
        assertEquals(Long.valueOf(1791138556229L), message.getEventTime());
        assertEquals(Integer.valueOf(1), message.getVersion());
        assertEquals(1004, message.getPayload().path("eventCode").asInt());
        assertNotNull(message.getMessageId());
    }

    @Test
    void missing_tenant_id_is_rejected() {
        DeviceInfo device = DeviceInfo.builder().sn("SN1").deviceType("dh-wt2ga").build();

        assertThrows(IllegalArgumentException.class,
                () -> Envelope.of(device, "ALARM", 1L, objectMapper.createObjectNode()));
    }
}
