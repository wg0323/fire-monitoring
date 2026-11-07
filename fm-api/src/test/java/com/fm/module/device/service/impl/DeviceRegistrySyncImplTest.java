package com.fm.module.device.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fm.module.device.entity.Device;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 注册表同步测试：写哈希时补 aepId 字段与 fm:device:aep:{aepId} 反查键，删除时同步清理。
 */
@ExtendWith(MockitoExtension.class)
class DeviceRegistrySyncImplTest {

    private static final String SN = "SN1";
    private static final String AEP_ID = "AEP1";

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private HashOperations<String, Object, Object> hashOperations;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private SetOperations<String, String> setOperations;

    private DeviceRegistrySyncImpl registrySync;

    @BeforeEach
    void setUp() {
        registrySync = new DeviceRegistrySyncImpl(redisTemplate, new ObjectMapper());
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
    }

    @Test
    void writes_aep_id_field_and_reverse_key() {
        registrySync.refresh(device("{\"aepId\":\"" + AEP_ID + "\"}"));

        ArgumentCaptor<Map<String, String>> captor = mapCaptor();
        verify(hashOperations).putAll(eq("fm:device:" + SN), captor.capture());
        assertEquals(AEP_ID, captor.getValue().get("aepId"));
        assertEquals("1", captor.getValue().get("tenantId"));
        verify(valueOperations).set("fm:device:aep:" + AEP_ID, SN);
    }

    @Test
    void invalid_f_ext_keeps_hash_write_without_reverse_key() {
        registrySync.refresh(device("not-json"));

        ArgumentCaptor<Map<String, String>> captor = mapCaptor();
        verify(hashOperations).putAll(eq("fm:device:" + SN), captor.capture());
        assertFalse(captor.getValue().containsKey("aepId"));
        verify(valueOperations, never()).set(anyString(), anyString());
    }

    @Test
    void missing_aep_id_skips_reverse_key() {
        registrySync.refresh(device("{\"other\":1}"));

        verify(hashOperations).putAll(eq("fm:device:" + SN), anyMap());
        verify(valueOperations, never()).set(anyString(), anyString());
    }

    @Test
    void remove_deletes_reverse_key_as_well() {
        when(hashOperations.get("fm:device:" + SN, "aepId")).thenReturn(AEP_ID);

        registrySync.remove(SN);

        verify(redisTemplate).delete("fm:device:" + SN);
        verify(redisTemplate).delete("fm:device:aep:" + AEP_ID);
        verify(setOperations).remove("fm:device:all", SN);
    }

    @Test
    void remove_without_aep_id_only_deletes_main_key() {
        when(hashOperations.get("fm:device:" + SN, "aepId")).thenReturn(null);

        registrySync.remove(SN);

        verify(redisTemplate).delete("fm:device:" + SN);
        verify(redisTemplate, never()).delete("fm:device:aep:null");
        verify(setOperations).remove("fm:device:all", SN);
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Map<String, String>> mapCaptor() {
        return ArgumentCaptor.forClass(Map.class);
    }

    private Device device(String fExt) {
        Device device = new Device();
        device.setSn(SN);
        device.setTenantId(1L);
        device.setType("dh-wt2ga");
        device.setOnlineStatus(1);
        device.setStatus(1);
        device.setFExt(fExt);
        return device;
    }
}
