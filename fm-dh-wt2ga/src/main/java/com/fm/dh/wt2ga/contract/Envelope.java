package com.fm.dh.wt2ga.contract;

import com.fasterxml.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * 统一信封构造：messageId 由「设备 + 消息类型 + 服务 + 时间戳 + 报文摘要」确定性生成，
 * 平台重复推送同一份报文时得到同一个 id，交由 fm-api 的唯一索引去重。
 */
public final class Envelope {

    private static final int VERSION = 1;

    private Envelope() {
    }

    public static DeviceMessage of(DeviceInfo device, String type, long eventTime, JsonNode payload) {
        return of(device, type, eventTime, payload, null, payload == null ? null : payload.toString());
    }

    public static DeviceMessage of(DeviceInfo device, String type, long eventTime, JsonNode payload,
                                   Object serviceId, String rawBody) {
        if (device == null || device.getTenantId() == null) {
            throw new IllegalArgumentException("device tenantId is required");
        }
        DeviceMessage message = new DeviceMessage();
        message.setMessageId(deterministicId(device.getSn(), type, serviceId, eventTime, rawBody));
        message.setSn(device.getSn());
        message.setTenantId(device.getTenantId());
        message.setType(type);
        message.setEventTime(eventTime);
        message.setVersion(VERSION);
        message.setPayload(payload);
        return message;
    }

    public static String deterministicId(String deviceId, String messageType, Object serviceId,
                                         Long timestamp, String rawBody) {
        String seed = deviceId + "|" + messageType + "|" + serviceId + "|" + timestamp + "|"
                + sha256(rawBody == null ? "" : rawBody);
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                hex.append(Character.forDigit((value >> 4) & 0x0F, 16));
                hex.append(Character.forDigit(value & 0x0F, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
