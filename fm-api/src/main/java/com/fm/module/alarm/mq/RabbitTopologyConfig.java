package com.fm.module.alarm.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 声明《MQ消息契约》中的交换机、队列与绑定。所有队列 durable=true。
 * 注意：RabbitMQ 不允许在线修改已存在队列的 arguments，否则声明时抛 PRECONDITION_FAILED，
 * 应用启动失败。因此 device.realtime / command.ack 两个存量业务队列保持原始无参声明，
 * 消费失败的消息由各自 Listener 显式投递到对应 DLQ（DLQ 带 TTL 与最大长度）。
 * 只有存量就已带死信参数的 device.up.q 继续由 broker 死信转发。
 */
@Configuration
public class RabbitTopologyConfig {

    @Bean
    public DirectExchange deviceUpExchange() {
        return new DirectExchange(MqConstants.EXCHANGE_DEVICE_UP, true, false);
    }

    @Bean
    public Queue deviceUpQueue() {
        return new Queue(MqConstants.QUEUE_DEVICE_UP, true, false, false, deadLetterArgs(
                MqConstants.EXCHANGE_DEVICE_UP, MqConstants.RK_DEVICE_UP_DLQ));
    }

    @Bean
    public Binding deviceUpBinding(Queue deviceUpQueue, DirectExchange deviceUpExchange) {
        return BindingBuilder.bind(deviceUpQueue).to(deviceUpExchange).with(MqConstants.RK_DEVICE_UP);
    }

    @Bean
    public Queue deviceUpDlq() {
        return new Queue(MqConstants.QUEUE_DEVICE_UP_DLQ, true);
    }

    @Bean
    public Binding deviceUpDlqBinding(Queue deviceUpDlq, DirectExchange deviceUpExchange) {
        return BindingBuilder.bind(deviceUpDlq).to(deviceUpExchange).with(MqConstants.RK_DEVICE_UP_DLQ);
    }

    @Bean
    public DirectExchange deviceRealtimeExchange() {
        return new DirectExchange(MqConstants.EXCHANGE_DEVICE_REALTIME, true, false);
    }

    @Bean
    public Queue deviceRealtimeQueue() {
        return new Queue(MqConstants.QUEUE_DEVICE_REALTIME, true);
    }

    @Bean
    public Binding deviceRealtimeBinding(Queue deviceRealtimeQueue, DirectExchange deviceRealtimeExchange) {
        return BindingBuilder.bind(deviceRealtimeQueue).to(deviceRealtimeExchange)
                .with(MqConstants.RK_DEVICE_REALTIME);
    }

    @Bean
    public Queue deviceRealtimeDlq() {
        return new Queue(MqConstants.QUEUE_DEVICE_REALTIME_DLQ, true, false, false, dlqArgs());
    }

    @Bean
    public Binding deviceRealtimeDlqBinding(Queue deviceRealtimeDlq, DirectExchange deviceRealtimeExchange) {
        return BindingBuilder.bind(deviceRealtimeDlq).to(deviceRealtimeExchange)
                .with(MqConstants.RK_DEVICE_REALTIME_DLQ);
    }

    @Bean
    public TopicExchange commandDownExchange() {
        return new TopicExchange(MqConstants.EXCHANGE_COMMAND_DOWN, true, false);
    }

    @Bean
    public DirectExchange commandAckExchange() {
        return new DirectExchange(MqConstants.EXCHANGE_COMMAND_ACK, true, false);
    }

    @Bean
    public Queue commandAckQueue() {
        return new Queue(MqConstants.QUEUE_COMMAND_ACK, true);
    }

    @Bean
    public Binding commandAckBinding(Queue commandAckQueue, DirectExchange commandAckExchange) {
        return BindingBuilder.bind(commandAckQueue).to(commandAckExchange).with(MqConstants.RK_COMMAND_ACK);
    }

    @Bean
    public Queue commandAckDlq() {
        return new Queue(MqConstants.QUEUE_COMMAND_ACK_DLQ, true, false, false, dlqArgs());
    }

    @Bean
    public Binding commandAckDlqBinding(Queue commandAckDlq, DirectExchange commandAckExchange) {
        return BindingBuilder.bind(commandAckDlq).to(commandAckExchange)
                .with(MqConstants.RK_COMMAND_ACK_DLQ);
    }

    /** 业务队列死信参数：复用各自主交换机作为 DLX，与 device.up 现有约定保持一致 */
    private static Map<String, Object> deadLetterArgs(String exchange, String routingKey) {
        Map<String, Object> args = new HashMap<>(2);
        args.put(MqConstants.ARG_DEAD_LETTER_EXCHANGE, exchange);
        args.put(MqConstants.ARG_DEAD_LETTER_ROUTING_KEY, routingKey);
        return args;
    }

    /** 死信队列保护参数：到期自动淘汰、超出长度从队头丢弃，避免无限堆积 */
    private static Map<String, Object> dlqArgs() {
        Map<String, Object> args = new HashMap<>(2);
        args.put(MqConstants.ARG_MESSAGE_TTL, MqConstants.DLQ_MESSAGE_TTL_MILLIS);
        args.put(MqConstants.ARG_MAX_LENGTH, MqConstants.DLQ_MAX_LENGTH);
        return args;
    }
}
