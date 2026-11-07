package com.fm.dh.wt2ga.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 上行交换机声明：只有交换机是采集器的责任，队列与绑定归 fm-api。
 * 交换机名、类型与路由键与 fm-api 的 MqConstants / RabbitTopologyConfig 逐字一致，
 * 类型不一致时 broker 会以 PRECONDITION_FAILED 拒绝声明。
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_DEVICE_UP = "fm.device.up";
    public static final String ROUTING_DEVICE_UP = "device.up";
    public static final String EXCHANGE_DEVICE_REALTIME = "fm.device.realtime";
    public static final String ROUTING_DEVICE_REALTIME = "device.realtime";

    @Bean
    public DirectExchange deviceUpExchange() {
        return new DirectExchange(EXCHANGE_DEVICE_UP, true, false);
    }

    @Bean
    public DirectExchange deviceRealtimeExchange() {
        return new DirectExchange(EXCHANGE_DEVICE_REALTIME, true, false);
    }

    /**
     * 上行消息必须发 JSON：fm-api 消费端直接对 message.getBody() 做 JSON 反序列化，
     * 默认的 SimpleMessageConverter 会把 DeviceMessage 按 Java 序列化发送，消费端无法处理。
     */
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
