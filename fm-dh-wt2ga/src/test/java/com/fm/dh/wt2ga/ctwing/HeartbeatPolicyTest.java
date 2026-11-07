package com.fm.dh.wt2ga.ctwing;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 心跳策略测试：只把上线当心跳，下线交给 last_time 扫描，避免周期唤醒导致在线状态抖动。
 */
@ExtendWith(MockitoExtension.class)
class HeartbeatPolicyTest {

    private static final String SN = "864852062800536";
    private static final long TIMESTAMP = 1791129720675L;
    private static final String RAW = "{\"messageType\":\"deviceOnlineOfflineReport\"}";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private MessagePublisher publisher;

    private HeartbeatPolicy policy;
    private DeviceInfo device;

    @BeforeEach
    void setUp() {
        policy = new HeartbeatPolicy(objectMapper, publisher);
        device = DeviceInfo.builder().sn(SN).aepId("99014977864852062800536").tenantId(1L).deviceType("dh-wt2ga").build();
    }

    @Test
    void online_maps_to_heartbeat_with_client_ip() {
        DeviceMessage message = policy.map(push(1, "117.61.14.111"), device, RAW)
                .orElseThrow(IllegalStateException::new);

        assertEquals("HEARTBEAT", message.getType());
        assertTrue(message.getPayload().path("online").asBoolean());
        assertEquals("117.61.14.111", message.getPayload().path("ip").asText());
        assertEquals(TIMESTAMP, message.getEventTime().longValue());
        verify(publisher).publishUp(same(message));
    }

    @Test
    void offline_is_logged_but_not_published() {
        assertFalse(policy.map(push(0, null), device, RAW).isPresent());

        verify(publisher, never()).publishUp(any(DeviceMessage.class));
    }

    @Test
    void missing_event_type_is_ignored() {
        assertFalse(policy.map(push(null, "117.61.14.111"), device, RAW).isPresent());

        verify(publisher, never()).publishUp(any(DeviceMessage.class));
    }

    private CtwingPushMessage push(Integer eventType, String ipv4Address) {
        CtwingPushMessage push = new CtwingPushMessage();
        push.setTimestamp(TIMESTAMP);
        push.setMessageType("deviceOnlineOfflineReport");
        push.setEventType(eventType);
        push.setIpv4Address(ipv4Address);
        push.setDeviceId("99014977864852062800536");
        return push;
    }
}
