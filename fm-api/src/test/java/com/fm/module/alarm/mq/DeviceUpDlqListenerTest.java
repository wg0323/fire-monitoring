package com.fm.module.alarm.mq;

import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 死信消费端测试：manual ACK 模式下必须确认消息，且缺死信头时也不能卡住消费。
 */
class DeviceUpDlqListenerTest {

    private static final long DELIVERY_TAG = 7L;

    private final Channel channel = mock(Channel.class);
    private final DeviceUpDlqListener listener = new DeviceUpDlqListener();

    @Test
    void deadLetterIsAckedWithFirstDeathHeader() throws Exception {
        MessageProperties properties = properties();
        properties.setHeader("x-first-death-routing-key", "device.up");

        listener.onMessage(new Message("sensitive-body".getBytes(StandardCharsets.UTF_8), properties), channel);

        verify(channel).basicAck(DELIVERY_TAG, false);
    }

    @Test
    void deadLetterIsAckedWithoutDeathHeader() throws Exception {
        listener.onMessage(new Message("sensitive-body".getBytes(StandardCharsets.UTF_8), properties()), channel);

        verify(channel).basicAck(DELIVERY_TAG, false);
    }

    private MessageProperties properties() {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(DELIVERY_TAG);
        properties.setMessageId("msg-1");
        properties.setReceivedRoutingKey(MqConstants.RK_DEVICE_UP_DLQ);
        return properties;
    }
}
