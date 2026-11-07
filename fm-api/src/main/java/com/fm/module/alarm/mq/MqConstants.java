package com.fm.module.alarm.mq;

/**
 * MQ 交换机、队列与路由键常量，与《MQ消息契约》保持一致。
 */
public final class MqConstants {

    /** 队列声明参数名，集中定义避免各处散落字符串字面量 */
    public static final String ARG_DEAD_LETTER_EXCHANGE = "x-dead-letter-exchange";
    public static final String ARG_DEAD_LETTER_ROUTING_KEY = "x-dead-letter-routing-key";
    public static final String ARG_MESSAGE_TTL = "x-message-ttl";
    public static final String ARG_MAX_LENGTH = "x-max-length";

    /** 死信队列保留 7 天、最多 1 万条，避免无人处理时无限堆积 */
    public static final long DLQ_MESSAGE_TTL_MILLIS = 7L * 24 * 60 * 60 * 1000;
    public static final int DLQ_MAX_LENGTH = 10000;

    public static final String EXCHANGE_DEVICE_UP = "fm.device.up";
    public static final String QUEUE_DEVICE_UP = "fm.device.up.q";
    public static final String RK_DEVICE_UP = "device.up";
    public static final String QUEUE_DEVICE_UP_DLQ = "fm.device.up.dlq";
    public static final String RK_DEVICE_UP_DLQ = "device.up.dlq";

    public static final String EXCHANGE_DEVICE_REALTIME = "fm.device.realtime";
    public static final String QUEUE_DEVICE_REALTIME = "fm.device.realtime.q";
    public static final String RK_DEVICE_REALTIME = "device.realtime";
    public static final String QUEUE_DEVICE_REALTIME_DLQ = "fm.device.realtime.dlq";
    public static final String RK_DEVICE_REALTIME_DLQ = "device.realtime.dlq";

    public static final String EXCHANGE_COMMAND_DOWN = "fm.command.down";
    public static final String EXCHANGE_COMMAND_ACK = "fm.command.ack";
    public static final String QUEUE_COMMAND_ACK = "fm.command.ack.q";
    public static final String RK_COMMAND_ACK = "command.ack";
    public static final String QUEUE_COMMAND_ACK_DLQ = "fm.command.ack.dlq";
    public static final String RK_COMMAND_ACK_DLQ = "command.ack.dlq";

    /**
     * 下行指令投递中间态：任务已随事务落库、等待 broker confirm。
     * 该状态由发布确认驱动，故在 MQ 契约常量中集中定义，供指令服务与超时任务共用。
     */
    public static final String COMMAND_STATUS_SENDING = "SENDING";

    public static final String TYPE_ALARM = "ALARM";
    public static final String TYPE_RECOVER = "RECOVER";
    public static final String TYPE_REALTIME = "REALTIME";
    public static final String TYPE_HEARTBEAT = "HEARTBEAT";
    public static final String TYPE_COMMAND_DOWN = "COMMAND_DOWN";
    public static final String TYPE_COMMAND_ACK = "COMMAND_ACK";

    private MqConstants() {
    }
}
