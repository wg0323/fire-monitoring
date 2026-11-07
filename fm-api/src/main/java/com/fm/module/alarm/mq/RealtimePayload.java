package com.fm.module.alarm.mq;

import lombok.Data;

import java.io.Serializable;

/**
 * REALTIME 消息负载：设备类型 + 实时数据 + 上报时间。
 */
@Data
public class RealtimePayload implements Serializable {

    private static final long serialVersionUID = 1L;

    private String deviceType;
    private String data;
    private Long reportTime;
}
