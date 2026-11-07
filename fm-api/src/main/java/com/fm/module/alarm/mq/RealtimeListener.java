package com.fm.module.alarm.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fm.module.device.service.DeviceRealtimeService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 实时快照消费端：REALTIME 走独立队列，不要求幂等。
 * 存量队列不能加 DLX 参数，消费失败时由本类显式投递到 DLQ 留痕。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimeListener {

    private final ObjectMapper objectMapper;
    private final DeviceRealtimeService realtimeService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = MqConstants.QUEUE_DEVICE_REALTIME, concurrency = "1")
    public void onMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            DeviceMessage deviceMessage = objectMapper.readValue(message.getBody(), DeviceMessage.class);
            if (!MqConstants.TYPE_REALTIME.equals(deviceMessage.getType())) {
                log.warn("non-realtime message on realtime queue dropped, type={}, messageId={}",
                        deviceMessage.getType(), deviceMessage.getMessageId());
            } else {
                realtimeService.save(deviceMessage);
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            log.error("realtime message consume failed, messageId={}",
                    message.getMessageProperties().getMessageId(), ex);
            sendToDlq(message);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /** 存量队列无 DLX 参数，显式投递到 DLQ 留痕；发送失败只记录日志，原消息仍按不重入队处理。 */
    private void sendToDlq(Message message) {
        try {
            rabbitTemplate.send(MqConstants.EXCHANGE_DEVICE_REALTIME,
                    MqConstants.RK_DEVICE_REALTIME_DLQ, message);
        } catch (RuntimeException sendEx) {
            log.error("realtime message forward to dlq failed", sendEx);
        }
    }
}
