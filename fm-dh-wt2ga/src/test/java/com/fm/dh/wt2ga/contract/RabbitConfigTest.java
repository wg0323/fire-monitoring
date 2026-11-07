package com.fm.dh.wt2ga.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 拓扑测试：交换机名与类型必须与 fm-api 一致，否则 broker 声明时报 PRECONDITION_FAILED。
 */
class RabbitConfigTest {

    private final RabbitConfig config = new RabbitConfig();

    @Test
    void device_up_exchange_matches_fm_api_topology() {
        assertExchange(config.deviceUpExchange(), "fm.device.up");
    }

    @Test
    void device_realtime_exchange_matches_fm_api_topology() {
        assertExchange(config.deviceRealtimeExchange(), "fm.device.realtime");
    }

    @Test
    void message_is_published_as_json() {
        MessageConverter converter = config.jsonMessageConverter(new ObjectMapper());
        DeviceMessage message = new DeviceMessage();
        message.setMessageId("m1");
        message.setSn("SN1");

        Message amqpMessage = converter.toMessage(message, new MessageProperties());

        assertTrue(converter instanceof Jackson2JsonMessageConverter);
        assertEquals(MessageProperties.CONTENT_TYPE_JSON, amqpMessage.getMessageProperties().getContentType());
        String body = new String(amqpMessage.getBody(), StandardCharsets.UTF_8);
        assertTrue(body.contains("\"messageId\":\"m1\""));
        assertTrue(body.contains("\"sn\":\"SN1\""));
    }

    private void assertExchange(Exchange exchange, String expectedName) {
        assertEquals(expectedName, exchange.getName());
        assertEquals(ExchangeTypes.DIRECT, exchange.getType());
        assertTrue(exchange.isDurable());
        assertFalse(exchange.isAutoDelete());
    }
}
