package com.fm.module.alarm.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fm.common.context.TenantContext;
import com.fm.module.alarm.service.CommandService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 下行指令回执消费端：按消息 tenantId 恢复租户上下文后更新任务状态。
 * 存量队列不能加 DLX 参数，消费失败时由本类显式投递到 DLQ 留痕。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommandAckListener {

    private final ObjectMapper objectMapper;
    private final CommandService commandService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = MqConstants.QUEUE_COMMAND_ACK, concurrency = "1")
    public void onMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            DeviceMessage deviceMessage = objectMapper.readValue(message.getBody(), DeviceMessage.class);
            try {
                if (deviceMessage.getTenantId() != null) {
                    TenantContext.set(deviceMessage.getTenantId(), "MQ", "ALL", false);
                }
                commandService.handleAck(deviceMessage);
            } finally {
                TenantContext.clear();
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            log.error("command ack consume failed, messageId={}",
                    message.getMessageProperties().getMessageId(), ex);
            sendToDlq(message);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /** 存量队列无 DLX 参数，显式投递到 DLQ 留痕；发送失败只记录日志，原消息仍按不重入队处理。 */
    private void sendToDlq(Message message) {
        try {
            rabbitTemplate.send(MqConstants.EXCHANGE_COMMAND_ACK,
                    MqConstants.RK_COMMAND_ACK_DLQ, message);
        } catch (RuntimeException sendEx) {
            log.error("command ack forward to dlq failed", sendEx);
        }
    }
}
