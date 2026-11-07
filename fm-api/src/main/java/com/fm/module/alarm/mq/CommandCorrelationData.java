package com.fm.module.alarm.mq;

import lombok.Getter;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 下行指令发布确认上下文：携带有限重投所需的 exchange/routingKey/原始消息，
 * 以及仅用于日志排查的载荷长度与摘要。任何日志与 toString 均不得输出消息体原文。
 */
@Getter
public class CommandCorrelationData extends CorrelationData {

    /** 有限重投次数上限，达到后转任务失败等待补偿 */
    public static final int MAX_RETRY = 3;

    private static final String DIGEST_ALGORITHM = "SHA-256";
    private static final int DIGEST_BYTES = 8;
    private static final String HEX = "0123456789abcdef";
    private static final String DIGEST_UNAVAILABLE = "unavailable";

    private final Long taskId;
    private final String commandId;
    private final String exchange;
    private final String routingKey;
    private final Message message;
    private final int payloadLength;
    private final String payloadDigest;
    private final AtomicInteger retryCount = new AtomicInteger(0);

    public CommandCorrelationData(Long taskId, String exchange, String routingKey, Message message) {
        super(String.valueOf(taskId));
        this.taskId = taskId;
        this.commandId = String.valueOf(taskId);
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.message = message;
        byte[] body = message == null ? new byte[0] : message.getBody();
        this.payloadLength = body.length;
        this.payloadDigest = digest(body);
    }

    private static String digest(byte[] body) {
        try {
            byte[] hash = MessageDigest.getInstance(DIGEST_ALGORITHM).digest(body);
            StringBuilder builder = new StringBuilder(DIGEST_BYTES * 2);
            for (int i = 0; i < DIGEST_BYTES; i++) {
                int value = hash[i] & 0xff;
                builder.append(HEX.charAt(value >>> 4)).append(HEX.charAt(value & 0x0f));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            return DIGEST_UNAVAILABLE;
        }
    }

    /**
     * 仅输出元数据：父类默认实现会带上 returnedMessage，可能把消息体原文带进日志。
     */
    @Override
    public String toString() {
        return "CommandCorrelationData{"
                + "commandId=" + commandId
                + ", taskId=" + taskId
                + ", exchange=" + exchange
                + ", routingKey=" + routingKey
                + ", payloadLength=" + payloadLength
                + ", payloadDigest=" + payloadDigest
                + ", retryCount=" + retryCount.get()
                + '}';
    }
}
