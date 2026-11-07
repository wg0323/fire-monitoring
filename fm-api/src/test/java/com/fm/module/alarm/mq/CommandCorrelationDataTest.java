package com.fm.module.alarm.mq;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * 发布确认上下文测试：保存重投信息，且任何字符串化都不得泄露消息体原文。
 */
class CommandCorrelationDataTest {

    private static final String SECRET_BODY = "{\"phone\":\"13800000000\"}";
    private static final Long TASK_ID = 123L;
    private static final String EXCHANGE = "fm.command.down";
    private static final String ROUTING_KEY = "command.test";

    @Test
    void contextKeepsResendInformation() {
        Message message = message();
        CommandCorrelationData data = new CommandCorrelationData(TASK_ID, EXCHANGE, ROUTING_KEY, message);

        assertEquals("123", data.getCommandId());
        assertEquals(TASK_ID, data.getTaskId());
        assertEquals(EXCHANGE, data.getExchange());
        assertEquals(ROUTING_KEY, data.getRoutingKey());
        assertSame(message, data.getMessage());
        assertEquals(SECRET_BODY.getBytes(StandardCharsets.UTF_8).length, data.getPayloadLength());
    }

    @Test
    void digestAndToStringNeverExposeBody() {
        CommandCorrelationData data = new CommandCorrelationData(TASK_ID, EXCHANGE, ROUTING_KEY, message());

        assertFalse(data.getPayloadDigest().isEmpty());
        assertFalse(data.getPayloadDigest().contains(SECRET_BODY));
        assertFalse(data.toString().contains("13800000000"));
        assertFalse(data.toString().contains(SECRET_BODY));
    }

    @Test
    void retryCountStartsAtZero() {
        CommandCorrelationData data = new CommandCorrelationData(TASK_ID, EXCHANGE, ROUTING_KEY, message());

        assertEquals(0, data.getRetryCount().get());
        assertEquals(CommandCorrelationData.MAX_RETRY, 3);
    }

    private Message message() {
        return new Message(SECRET_BODY.getBytes(StandardCharsets.UTF_8), new MessageProperties());
    }
}
