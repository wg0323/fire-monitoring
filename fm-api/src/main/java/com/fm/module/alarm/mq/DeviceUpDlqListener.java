package com.fm.module.alarm.mq;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * device.up 死信消费端：只做结构化记录与确认，严禁自动重投主队列。
 * 全局 acknowledge-mode=manual，故显式 basicAck；记录成功与否都确认，避免死信再次堆积。
 */
@Slf4j
@Component
public class DeviceUpDlqListener {

    private static final String HEADER_FIRST_DEATH_ROUTING_KEY = "x-first-death-routing-key";
    private static final String HEADER_X_DEATH = "x-death";
    private static final String X_DEATH_ROUTING_KEYS = "routing-keys";
    private static final String UNKNOWN_ROUTING_KEY = "unknown";

    @RabbitListener(queues = MqConstants.QUEUE_DEVICE_UP_DLQ, concurrency = "1")
    public void onMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            byte[] body = message.getBody();
            log.error("device up dead letter received, messageId={}, originalRoutingKey={}, "
                            + "dlqRoutingKey={}, bodyLength={}",
                    message.getMessageProperties().getMessageId(),
                    originalRoutingKey(message),
                    message.getMessageProperties().getReceivedRoutingKey(),
                    body == null ? 0 : body.length);
            // 人工重放扩展点见 replay()，此处故意不自动调用，避免死信循环
        } finally {
            channel.basicAck(deliveryTag, false);
        }
    }

    /** 从 RabbitMQ 死信头还原原始路由键；取不到返回 unknown，绝不解析或输出消息体 */
    private String originalRoutingKey(Message message) {
        MessageProperties properties = message.getMessageProperties();
        Object firstDeath = properties.getHeader(HEADER_FIRST_DEATH_ROUTING_KEY);
        if (firstDeath != null) {
            return String.valueOf(firstDeath);
        }
        Object xDeath = properties.getHeader(HEADER_X_DEATH);
        if (xDeath instanceof List) {
            for (Object entry : (List<?>) xDeath) {
                if (!(entry instanceof Map)) {
                    continue;
                }
                Object keys = ((Map<?, ?>) entry).get(X_DEATH_ROUTING_KEYS);
                if (keys instanceof List && !((List<?>) keys).isEmpty()) {
                    return String.valueOf(((List<?>) keys).get(0));
                }
            }
        }
        return UNKNOWN_ROUTING_KEY;
    }

    /**
     * 人工重放扩展点：仅保留入口，当前为空实现且不自动调用。
     * 死信消息若自动重投主队列会形成「消费失败 -> 死信 -> 重投 -> 再失败」的循环；
     * 后续应由运维确认根因后，通过独立接口按 messageId 单条重放，并带重放次数上限。
     */
    @SuppressWarnings("unused")
    private void replay(Message message, String targetExchange, String targetRoutingKey) {
        // 预留：落库待人工处理 / 运维触发的单条重投。
    }
}
