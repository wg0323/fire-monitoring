package com.fm.dh.wt2ga.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.CorrelationData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 发布器测试：路由、幂等关联 id 与三次退避重试。
 */
@ExtendWith(MockitoExtension.class)
class MessagePublisherTest {

    private static final String MESSAGE_ID = "message-1";

    @Mock
    private RabbitTemplate rabbitTemplate;

    private MessagePublisher publisher;

    @BeforeEach
    void setUp() {
        // 测试里把退避间隔压到 0，避免等待真实 1s / 2s / 4s
        publisher = new MessagePublisher(rabbitTemplate, new long[]{0L, 0L, 0L});
    }

    @Test
    void alarm_goes_to_device_up_route() {
        DeviceMessage message = message("ALARM");

        publisher.publishUp(message);

        verify(rabbitTemplate).convertAndSend(eq("fm.device.up"), eq("device.up"),
                same(message), any(CorrelationData.class));
    }

    @Test
    void realtime_goes_to_device_realtime_route() {
        DeviceMessage message = message("REALTIME");

        publisher.publishRealtime(message);

        verify(rabbitTemplate).convertAndSend(eq("fm.device.realtime"), eq("device.realtime"),
                same(message), any(CorrelationData.class));
    }

    @Test
    void correlation_data_carries_message_id() {
        publisher.publishUp(message("ALARM"));

        ArgumentCaptor<CorrelationData> captor = ArgumentCaptor.forClass(CorrelationData.class);
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(DeviceMessage.class), captor.capture());
        assertEquals(MESSAGE_ID, captor.getValue().getId());
    }

    @Test
    void retries_three_times_then_logs_error() {
        doThrow(new AmqpConnectException(new RuntimeException("down")))
                .when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(DeviceMessage.class), any(CorrelationData.class));

        publisher.publishUp(message("ALARM"));

        verify(rabbitTemplate, times(4))
                .convertAndSend(anyString(), anyString(), any(DeviceMessage.class), any(CorrelationData.class));
    }

    @Test
    void stops_retrying_after_first_success() {
        doThrow(new AmqpConnectException(new RuntimeException("down")))
                .doNothing()
                .when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(DeviceMessage.class), any(CorrelationData.class));

        publisher.publishUp(message("ALARM"));

        verify(rabbitTemplate, times(2))
                .convertAndSend(anyString(), anyString(), any(DeviceMessage.class), any(CorrelationData.class));
    }

    private DeviceMessage message(String type) {
        DeviceMessage message = new DeviceMessage();
        message.setMessageId(MESSAGE_ID);
        message.setSn("SN1");
        message.setTenantId(1L);
        message.setType(type);
        message.setEventTime(1791138556229L);
        message.setVersion(1);
        message.setPayload(new ObjectMapper().createObjectNode());
        return message;
    }
}
