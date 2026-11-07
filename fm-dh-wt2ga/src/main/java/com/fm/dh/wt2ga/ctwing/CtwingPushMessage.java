package com.fm.dh.wt2ga.ctwing;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/**
 * CTWing 推送报文，只保留采集器用到的字段；其余字段由 Jackson 忽略。
 */
@Data
public class CtwingPushMessage {

    private Long timestamp;
    private String tenantId;
    private String protocol;
    private String productId;
    private String messageType;
    private Integer eventType;
    private String serviceId;
    private JsonNode eventContent;
    private JsonNode payload;
    private String deviceSn;
    private String deviceId;
    private String ipv4Address;
}
