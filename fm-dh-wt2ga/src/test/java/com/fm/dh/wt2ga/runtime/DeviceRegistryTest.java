package com.fm.dh.wt2ga.runtime;

import com.fm.dh.wt2ga.config.RegistryProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * 设备注册表测试。
 *
 * <p>容量按 5000 台设计：单轮刷新是 1 次 SMEMBERS + 5000 次 HGETALL，
 * 60 秒一轮约 83 次/秒，属于 Redis 低负载；因此这里不做基准测试，只验证索引与容错行为。</p>
 */
@ExtendWith(MockitoExtension.class)
class DeviceRegistryTest {

    private static final String KEY_ALL = "fm:device:all";
    private static final String SN = "SN1";
    private static final String AEP_ID = "AEP1";
    private static final String DEVICE_TYPE = "dh-wt2ga";

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private SetOperations<String, String> setOperations;
    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    private DeviceRegistry registry;

    @BeforeEach
    void setUp() {
        RegistryProperties properties = new RegistryProperties();
        properties.setDeviceType(DEVICE_TYPE);
        registry = new DeviceRegistry(redisTemplate, properties);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(setOperations.members(KEY_ALL)).thenReturn(new HashSet<>(Arrays.asList(SN)));
    }

    @Test
    void loads_both_indexes_from_redis() {
        when(hashOperations.entries("fm:device:" + SN)).thenReturn(hash(AEP_ID, "1", DEVICE_TYPE));

        registry.refresh();

        assertEquals(SN, registry.byAepId(AEP_ID).orElseThrow(IllegalStateException::new).getSn());
        assertEquals(Long.valueOf(1L), registry.bySn(SN).orElseThrow(IllegalStateException::new).getTenantId());
    }

    @Test
    void missing_aep_id_does_not_break_sn_index() {
        when(hashOperations.entries("fm:device:" + SN)).thenReturn(hash(null, "1", DEVICE_TYPE));

        registry.refresh();

        assertTrue(registry.bySn(SN).isPresent());
        assertFalse(registry.byAepId(AEP_ID).isPresent());
    }

    @Test
    void other_device_type_is_filtered_out() {
        when(hashOperations.entries("fm:device:" + SN)).thenReturn(hash(AEP_ID, "1", "dh-cms12gb"));

        registry.refresh();

        assertFalse(registry.bySn(SN).isPresent());
        assertFalse(registry.byAepId(AEP_ID).isPresent());
    }

    @Test
    void keeps_previous_snapshot_when_redis_fails() {
        when(hashOperations.entries("fm:device:" + SN)).thenReturn(hash(AEP_ID, "1", DEVICE_TYPE));
        registry.refresh();
        when(setOperations.members(KEY_ALL)).thenThrow(new RuntimeException("redis down"));

        registry.refresh();

        assertTrue(registry.bySn(SN).isPresent());
    }

    private Map<Object, Object> hash(String aepId, String tenantId, String deviceType) {
        Map<Object, Object> values = new HashMap<>(4);
        if (aepId != null) {
            values.put("aepId", aepId);
        }
        values.put("tenantId", tenantId);
        values.put("deviceType", deviceType);
        return values;
    }
}
