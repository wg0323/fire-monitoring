package com.fm.module.device.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fm.common.context.TenantContext;
import com.fm.module.alarm.mq.DeviceMessage;
import com.fm.module.device.entity.Device;
import com.fm.module.device.mapper.DeviceMapper;
import com.fm.module.device.service.DeviceRegistrySync;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 心跳入库测试：在线状态、最后上报时间与注册表刷新。
 */
class DeviceStatusServiceImplTest {

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "test");
        TableInfoHelper.initTableInfo(assistant, Device.class);
    }

    private static final String SN = "010203040506";
    private static final long TENANT_ID = 1L;
    private static final long EVENT_TIME = 1761196800000L;

    private final DeviceMapper deviceMapper = mock(DeviceMapper.class);
    private final DeviceRegistrySync registrySync = mock(DeviceRegistrySync.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DeviceStatusServiceImpl service =
            new DeviceStatusServiceImpl(deviceMapper, registrySync);

    @Test
    void heartbeatMarksDeviceOnline() {
        when(deviceMapper.selectOne(any())).thenReturn(device());

        service.handleHeartbeat(message(true));

        verify(deviceMapper).update(isNull(), any());
        ArgumentCaptor<Device> captor = ArgumentCaptor.forClass(Device.class);
        verify(registrySync).refresh(captor.capture());
        assertEquals(1, captor.getValue().getOnlineStatus());
        assertNotNull(captor.getValue().getLastTime());
        assertNull(TenantContext.getUserType());
    }

    @Test
    void offlineHeartbeatMarksDeviceOffline() {
        when(deviceMapper.selectOne(any())).thenReturn(device());

        service.handleHeartbeat(message(false));

        ArgumentCaptor<Device> captor = ArgumentCaptor.forClass(Device.class);
        verify(registrySync).refresh(captor.capture());
        assertEquals(0, captor.getValue().getOnlineStatus());
    }

    @Test
    void unregisteredDeviceIsDropped() {
        when(deviceMapper.selectOne(any())).thenReturn(null);

        service.handleHeartbeat(message(true));

        verify(deviceMapper, never()).update(isNull(), any());
        verify(registrySync, never()).refresh(any());
    }

    private Device device() {
        Device device = new Device();
        device.setId(1L);
        device.setTenantId(TENANT_ID);
        device.setSn(SN);
        return device;
    }

    private DeviceMessage message(boolean online) {
        DeviceMessage message = new DeviceMessage();
        message.setMessageId("heartbeat-1");
        message.setSn(SN);
        message.setTenantId(TENANT_ID);
        message.setType("HEARTBEAT");
        message.setEventTime(EVENT_TIME);
        message.setVersion(1);
        message.setPayload(objectMapper.createObjectNode().put("online", online));
        return message;
    }
}
