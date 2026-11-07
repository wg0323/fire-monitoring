package com.fm.dh.wt2ga.contract;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * MQ 发布器：两类上行消息分别投递到 device.up 与 device.realtime。
 * 发送失败按 1s / 2s / 4s 退避重试三次，仍失败则记 ERROR 丢弃（平台重推由确定性 messageId 兜底）。
 */
@Slf4j
@Component
public class MessagePublisher {

    private final RabbitTemplate rabbitTemplate;
    private final long[] retryDelaysMs;

    public MessagePublisher(RabbitTemplate rabbitTemplate,
                            @Value("${fm.publish.retry-delays-ms:1000,2000,4000}") long[] retryDelaysMs) {
        this.rabbitTemplate = rabbitTemplate;
        this.retryDelaysMs = retryDelaysMs;
        // 确认与退回回调只记日志：确认失败不在这里重发，重发由 publish 的同步重试负责
        rabbitTemplate.setConfirmCallback((correlation, ack, cause) -> {
            if (!ack) {
                log.warn("publisher confirm nack, messageId={}, cause={}",
                        correlation == null ? null : correlation.getId(), cause);
            }
        });
        rabbitTemplate.setReturnsCallback(returned -> log.error(
                "message returned by broker, exchange={}, routingKey={}, replyText={}",
                returned.getExchange(), returned.getRoutingKey(), returned.getReplyText()));
    }

    public void publishUp(DeviceMessage message) {
        publish(RabbitConfig.EXCHANGE_DEVICE_UP, RabbitConfig.ROUTING_DEVICE_UP, message);
    }

    public void publishRealtime(DeviceMessage message) {
        publish(RabbitConfig.EXCHANGE_DEVICE_REALTIME, RabbitConfig.ROUTING_DEVICE_REALTIME, message);
    }

    private void publish(String exchange, String routingKey, DeviceMessage message) {
        CorrelationData correlationData = new CorrelationData(message.getMessageId());
        for (int attempt = 0; attempt <= retryDelaysMs.length; attempt++) {
            try {
                rabbitTemplate.convertAndSend(exchange, routingKey, message, correlationData);
                return;
            } catch (RuntimeException ex) {
                if (attempt == retryDelaysMs.length) {
                    log.error("publish failed after {} attempts, messageId={}, routingKey={}",
                            attempt + 1, message.getMessageId(), routingKey, ex);
                    return;
                }
                log.warn("publish failed, retry in {}ms, messageId={}, routingKey={}",
                        retryDelaysMs[attempt], message.getMessageId(), routingKey, ex);
                sleep(retryDelaysMs[attempt]);
            }
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("publish retry interrupted");
        }
    }
}
