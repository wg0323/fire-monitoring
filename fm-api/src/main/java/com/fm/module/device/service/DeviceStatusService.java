package com.fm.module.device.service;

import com.fm.module.alarm.mq.DeviceMessage;

/**
 * 设备在线状态服务：消费 HEARTBEAT 消息刷新在线状态与最后上报时间。
 */
public interface DeviceStatusService {

    void handleHeartbeat(DeviceMessage message);
}
