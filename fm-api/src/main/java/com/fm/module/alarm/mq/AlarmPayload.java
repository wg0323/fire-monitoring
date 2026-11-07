package com.fm.module.alarm.mq;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.io.Serializable;

/**
 * ALARM / RECOVER 消息负载。
 */
@Data
public class AlarmPayload implements Serializable {

    private static final long serialVersionUID = 1L;

    private String alarmType;
    private Integer eventCode;
    private Integer level;
    private JsonNode data;
    private String raw;
}
