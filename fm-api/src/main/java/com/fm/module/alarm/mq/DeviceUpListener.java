package com.fm.module.alarm.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fm.module.alarm.service.AlarmService;
import com.fm.module.device.service.DeviceRealtimeService;
import com.fm.module.device.service.DeviceStatusService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 设备上行消息消费端：手动 ACK，业务失败转死信队列。
 * 单队列单消费者，保证同一设备的报警与恢复不乱序。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceUpListener {

    private final ObjectMapper objectMapper;
    private final AlarmService alarmService;
    private final DeviceStatusService deviceStatusService;
    private final DeviceRealtimeService deviceRealtimeService;

    @RabbitListener(queues = MqConstants.QUEUE_DEVICE_UP, concurrency = "1")
    public void onMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            DeviceMessage deviceMessage = objectMapper.readValue(message.getBody(), DeviceMessage.class);
            dispatch(deviceMessage);
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            log.error("device up message consume failed", ex);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private void dispatch(DeviceMessage message) {
        if (MqConstants.TYPE_ALARM.equals(message.getType())) {
            alarmService.handleAlarm(message);
        } else if (MqConstants.TYPE_RECOVER.equals(message.getType())) {
            alarmService.handleRecover(message);
        } else if (MqConstants.TYPE_HEARTBEAT.equals(message.getType())) {
            deviceStatusService.handleHeartbeat(message);
        } else if (MqConstants.TYPE_REALTIME.equals(message.getType())) {
            // 兼容老采集器把 REALTIME 投递到上行队列的情况
            deviceRealtimeService.save(message);
        } else {
            log.warn("unsupported message type: {}, messageId: {}", message.getType(), message.getMessageId());
        }
    }
}
