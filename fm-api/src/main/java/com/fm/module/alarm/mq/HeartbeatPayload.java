package com.fm.module.alarm.mq;

import lombok.Data;

import java.io.Serializable;

/**
 * HEARTBEAT 消息负载。
 */
@Data
public class HeartbeatPayload implements Serializable {

    private static final long serialVersionUID = 1L;

    private Boolean online;
    private String ip;
    private Integer signal;
}
