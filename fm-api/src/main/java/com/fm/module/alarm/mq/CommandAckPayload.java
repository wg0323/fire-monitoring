package com.fm.module.alarm.mq;

import lombok.Data;

import java.io.Serializable;

/**
 * COMMAND_ACK 消息负载：采集器执行下行指令后的回执。
 */
@Data
public class CommandAckPayload implements Serializable {

    private static final long serialVersionUID = 1L;

    private String commandId;
    private String status;
    private String result;
    private Long respTime;
}
