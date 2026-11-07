package com.fm.module.device.service;

import com.fm.module.device.entity.Device;

/**
 * 设备注册表同步：设备变更后刷新 Redis，供采集器加载。
 */
public interface DeviceRegistrySync {

    void refresh(Device device);

    void remove(String sn);
}
