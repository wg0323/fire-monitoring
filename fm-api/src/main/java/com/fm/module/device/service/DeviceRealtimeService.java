package com.fm.module.device.service;

import com.fm.module.alarm.mq.DeviceMessage;

/**
 * 设备实时快照服务：消费 REALTIME 消息写入 b_device_realtime。
 */
public interface DeviceRealtimeService {

    void save(DeviceMessage message);
}
