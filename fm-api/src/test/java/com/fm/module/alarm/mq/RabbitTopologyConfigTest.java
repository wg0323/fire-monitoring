package com.fm.module.alarm.mq;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 拓扑声明测试：业务队列必须挂死信，死信队列必须持久化且带 TTL/长度上限。
 */
class RabbitTopologyConfigTest {

    private final RabbitTopologyConfig config = new RabbitTopologyConfig();

    @Test
    void businessQueuesAreDurable() {
        assertDeadLetter(config.deviceUpQueue(), MqConstants.EXCHANGE_DEVICE_UP,
                MqConstants.RK_DEVICE_UP_DLQ);
        // 存量队列不得新增 arguments，否则已有环境启动会触发 PRECONDITION_FAILED
        assertLegacyQueueWithoutArgs(config.deviceRealtimeQueue());
        assertLegacyQueueWithoutArgs(config.commandAckQueue());
    }

    @Test
    void deadLetterQueuesAreDurableAndBounded() {
        assertDeadLetterQueue(config.deviceRealtimeDlq(), MqConstants.QUEUE_DEVICE_REALTIME_DLQ);
        assertDeadLetterQueue(config.commandAckDlq(), MqConstants.QUEUE_COMMAND_ACK_DLQ);
    }

    @Test
    void deadLetterBindingsUseDedicatedRoutingKeys() {
        assertEquals(MqConstants.RK_DEVICE_REALTIME_DLQ,
                config.deviceRealtimeDlqBinding(config.deviceRealtimeDlq(),
                        config.deviceRealtimeExchange()).getRoutingKey());
        assertEquals(MqConstants.RK_COMMAND_ACK_DLQ,
                config.commandAckDlqBinding(config.commandAckDlq(),
                        config.commandAckExchange()).getRoutingKey());
    }

    private void assertDeadLetter(Queue queue, String exchange, String routingKey) {
        assertTrue(queue.isDurable(), "业务队列必须 durable");
        assertEquals(exchange, queue.getArguments().get(MqConstants.ARG_DEAD_LETTER_EXCHANGE));
        assertEquals(routingKey, queue.getArguments().get(MqConstants.ARG_DEAD_LETTER_ROUTING_KEY));
    }

    private void assertLegacyQueueWithoutArgs(Queue queue) {
        assertTrue(queue.isDurable(), "业务队列必须 durable");
        assertTrue(queue.getArguments() == null || queue.getArguments().isEmpty(),
                "存量业务队列不得新增声明参数，否则已有环境启动会失败");
    }

    private void assertDeadLetterQueue(Queue queue, String name) {
        assertEquals(name, queue.getName());
        assertTrue(queue.isDurable(), "死信队列必须 durable");
        assertEquals(MqConstants.DLQ_MESSAGE_TTL_MILLIS,
                ((Number) queue.getArguments().get(MqConstants.ARG_MESSAGE_TTL)).longValue());
        assertEquals(MqConstants.DLQ_MAX_LENGTH,
                ((Number) queue.getArguments().get(MqConstants.ARG_MAX_LENGTH)).intValue());
    }
}
