package com.fm.dh.wt2ga.ctwing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fm.dh.wt2ga.contract.DeviceInfo;
import com.fm.dh.wt2ga.contract.DeviceMessage;
import com.fm.dh.wt2ga.contract.MessagePublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;

/**
 * 实时快照测试：设备把温度、电池、信号拆在不同 serviceId 上报，逐条发会让快照只剩最后一条，
 * 因此必须按 sn 合并后整包下发。
 */
@ExtendWith(MockitoExtension.class)
class RealtimeSnapshotTest {

    private static final String SN = "864852062800536";
    private static final long TIMESTAMP = 1791138556285L;
    private static final String RAW = "{\"messageType\":\"dataReport\"}";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private MessagePublisher publisher;

    private RealtimeSnapshot snapshot;
    private DeviceInfo device;

    @BeforeEach
    void setUp() {
        snapshot = new RealtimeSnapshot(objectMapper, publisher);
        device = DeviceInfo.builder().sn(SN).aepId("99014977864852062800536").tenantId(1L).deviceType("dh-wt2ga").build();
    }

    @Test
    void keeps_previous_fields_when_new_report_is_partial() throws Exception {
        snapshot.merge(push("temperature_report", "{\"temperature_value\":40.6,\"temperature_state\":1}"),
                device, RAW);

        DeviceMessage message = snapshot.merge(push("devicepara_report",
                "{\"battery_value\":100,\"signalPower\":28}"), device, RAW);

        JsonNode data = message.getPayload().path("data");
        assertEquals(40.6, data.path("temperature").asDouble());
        assertEquals(1, data.path("temperatureState").asInt());
        assertEquals(100, data.path("batteryValue").asInt());
        assertEquals(28, data.path("signalPower").asInt());
    }

    @Test
    void snapshot_is_published_whole_with_device_type_and_report_time() throws Exception {
        DeviceMessage message = snapshot.merge(push("temperature_report",
                "{\"temperature_value\":40.6,\"temperature_state\":1}"), device, RAW);

        assertEquals("REALTIME", message.getType());
        assertEquals("dh-wt2ga", message.getPayload().path("deviceType").asText());
        assertEquals(TIMESTAMP, message.getPayload().path("reportTime").asLong());
        assertEquals(SN, message.getSn());
        verify(publisher).publishRealtime(same(message));
    }

    @Test
    void signal_report_only_updates_signal_fields() throws Exception {
        snapshot.merge(push("temperature_report", "{\"temperature_value\":48.4,\"temperature_state\":0}"),
                device, RAW);

        DeviceMessage message = snapshot.merge(push("signal_report",
                "{\"sinr\":-32768,\"rsrp\":-32768,\"pci\":-32768,\"ecl\":-32768,\"cell_id\":210119696}"),
                device, RAW);

        JsonNode data = message.getPayload().path("data");
        assertEquals(48.4, data.path("temperature").asDouble());
        assertEquals(210119696, data.path("cellId").asInt());
        assertFalse(data.has("sinr"));
        assertFalse(data.has("rsrp"));
    }

    @Test
    void unknown_fields_are_ignored() throws Exception {
        DeviceMessage message = snapshot.merge(push("register_report",
                "{\"sn\":\"1\",\"IMSI\":\"1\",\"software_version\":\"1\"}"), device, RAW);

        assertTrue(message.getPayload().path("data").isObject());
        assertEquals(0, message.getPayload().path("data").size());
    }

    @Test
    void snapshots_are_isolated_by_sn() throws Exception {
        snapshot.merge(push("temperature_report", "{\"temperature_value\":40.6}"), device, RAW);
        DeviceInfo other = DeviceInfo.builder().sn("SN2").aepId("AEP2").tenantId(1L).deviceType("dh-wt2ga").build();

        DeviceMessage otherMessage = snapshot.merge(push("temperature_report", "{\"temperature_value\":22.0}"),
                other, RAW);

        assertEquals(1, otherMessage.getPayload().path("data").size());
        assertEquals(22.0, otherMessage.getPayload().path("data").path("temperature").asDouble());
    }

    private CtwingPushMessage push(String serviceId, String payload) throws Exception {
        CtwingPushMessage push = new CtwingPushMessage();
        push.setTimestamp(TIMESTAMP);
        push.setMessageType("dataReport");
        push.setServiceId(serviceId);
        push.setPayload(objectMapper.readTree(payload));
        push.setDeviceId("99014977864852062800536");
        return push;
    }
}
