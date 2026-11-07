package com.fm.module.alarm.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fm.module.alarm.service.AlarmService;
import com.fm.module.device.service.DeviceRealtimeService;
import com.fm.module.device.service.DeviceStatusService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 上行队列分派测试：不同类型必须路由到对应服务。
 */
class DeviceUpListenerTest {

    private static final long DELIVERY_TAG = 9L;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AlarmService alarmService = mock(AlarmService.class);
    private final DeviceStatusService statusService = mock(DeviceStatusService.class);
    private final DeviceRealtimeService realtimeService = mock(DeviceRealtimeService.class);
    private final Channel channel = mock(Channel.class);
    private final DeviceUpListener listener =
            new DeviceUpListener(objectMapper, alarmService, statusService, realtimeService);

    @Test
    void alarmMessageDispatchedToAlarmService() throws Exception {
        listener.onMessage(message("ALARM"), channel);

        verify(alarmService).handleAlarm(any());
        verify(channel).basicAck(DELIVERY_TAG, false);
    }

    @Test
    void heartbeatMessageDispatchedToStatusService() throws Exception {
        listener.onMessage(message("HEARTBEAT"), channel);

        verify(statusService).handleHeartbeat(any());
        verify(realtimeService, never()).save(any());
        verify(channel).basicAck(DELIVERY_TAG, false);
    }

    @Test
    void realtimeMessageDispatchedToRealtimeService() throws Exception {
        listener.onMessage(message("REALTIME"), channel);

        verify(realtimeService).save(any());
        verify(statusService, never()).handleHeartbeat(any());
        verify(channel).basicAck(DELIVERY_TAG, false);
    }

    @Test
    void unreadableMessageIsRejected() throws Exception {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(DELIVERY_TAG);
        Message bad = new Message("{bad-json".getBytes(StandardCharsets.UTF_8), properties);

        listener.onMessage(bad, channel);

        verify(channel).basicNack(DELIVERY_TAG, false, false);
    }

    private Message message(String type) throws Exception {
        DeviceMessage deviceMessage = new DeviceMessage();
        deviceMessage.setMessageId("message-1");
        deviceMessage.setSn("010203040506");
        deviceMessage.setTenantId(1L);
        deviceMessage.setType(type);
        deviceMessage.setEventTime(System.currentTimeMillis());
        deviceMessage.setVersion(1);
        deviceMessage.setPayload(objectMapper.createObjectNode().put("online", true));
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(DELIVERY_TAG);
        return new Message(objectMapper.writeValueAsBytes(deviceMessage), properties);
    }
}
