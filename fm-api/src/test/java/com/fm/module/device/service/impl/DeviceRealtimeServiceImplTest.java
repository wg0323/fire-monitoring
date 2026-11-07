package com.fm.module.device.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fm.common.context.TenantContext;
import com.fm.module.alarm.mq.DeviceMessage;
import com.fm.module.device.entity.Device;
import com.fm.module.device.entity.DeviceRealtime;
import com.fm.module.device.mapper.DeviceMapper;
import com.fm.module.device.mapper.DeviceRealtimeMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 实时快照入库测试：新增、覆盖、未注册设备与重复键回退。
 */
class DeviceRealtimeServiceImplTest {

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "test");
        TableInfoHelper.initTableInfo(assistant, Device.class);
        TableInfoHelper.initTableInfo(assistant, DeviceRealtime.class);
    }

    private static final String SN = "010203040506";
    private static final String DEVICE_TYPE = "dh-cms12gb";
    private static final long TENANT_ID = 1L;

    private final DeviceRealtimeMapper realtimeMapper = mock(DeviceRealtimeMapper.class);
    private final DeviceMapper deviceMapper = mock(DeviceMapper.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DeviceRealtimeServiceImpl service =
            new DeviceRealtimeServiceImpl(realtimeMapper, deviceMapper);

    @Test
    void insertWhenSnapshotAbsent() {
        when(deviceMapper.selectOne(any())).thenReturn(device());
        when(realtimeMapper.selectOne(any())).thenReturn(null);

        service.save(message());

        ArgumentCaptor<DeviceRealtime> captor = ArgumentCaptor.forClass(DeviceRealtime.class);
        verify(realtimeMapper).insert(captor.capture());
        DeviceRealtime snapshot = captor.getValue();
        assertEquals(SN, snapshot.getSn());
        assertEquals(DEVICE_TYPE, snapshot.getType());
        assertEquals(TENANT_ID, snapshot.getTenantId());
        assertTrue(snapshot.getData().contains("\"status\":1"));
        assertNotNull(snapshot.getReportTime());
        assertNull(TenantContext.getUserType());
    }

    @Test
    void updateWhenSnapshotExists() {
        when(deviceMapper.selectOne(any())).thenReturn(device());
        when(realtimeMapper.selectOne(any())).thenReturn(new DeviceRealtime());

        service.save(message());

        verify(realtimeMapper).update(isNull(), any());
        verify(realtimeMapper, never()).insert(any());
    }

    @Test
    void duplicateKeyFallsBackToUpdate() {
        when(deviceMapper.selectOne(any())).thenReturn(device());
        when(realtimeMapper.selectOne(any())).thenReturn(null);
        when(realtimeMapper.insert(any())).thenThrow(new DuplicateKeyException("duplicate"));

        service.save(message());

        verify(realtimeMapper).update(isNull(), any());
    }

    @Test
    void unregisteredDeviceIsDropped() {
        when(deviceMapper.selectOne(any())).thenReturn(null);

        service.save(message());

        verify(realtimeMapper, never()).insert(any());
        verify(realtimeMapper, never()).update(isNull(), any());
    }

    private Device device() {
        Device device = new Device();
        device.setId(1L);
        device.setTenantId(TENANT_ID);
        device.setSn(SN);
        device.setType(DEVICE_TYPE);
        return device;
    }

    private DeviceMessage message() {
        DeviceMessage message = new DeviceMessage();
        message.setMessageId("realtime-1");
        message.setSn(SN);
        message.setTenantId(TENANT_ID);
        message.setType("REALTIME");
        message.setEventTime(1761196800000L);
        message.setVersion(1);
        message.setPayload(objectMapper.createObjectNode()
                .put("deviceType", DEVICE_TYPE)
                .put("reportTime", 1761196800000L)
                .set("data", objectMapper.createObjectNode().put("status", 1)));
        return message;
    }
}
