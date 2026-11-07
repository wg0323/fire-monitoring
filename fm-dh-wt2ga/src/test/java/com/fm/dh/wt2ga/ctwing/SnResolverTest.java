package com.fm.dh.wt2ga.ctwing;

import com.fm.dh.wt2ga.config.RegistryProperties;
import com.fm.dh.wt2ga.contract.DeviceInfo;
import com.fm.dh.wt2ga.runtime.DeviceRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

/**
 * sn 解析测试：eventReport 带 deviceSn，dataReport 与上下线只有 deviceId，需要反查。
 */
@ExtendWith(MockitoExtension.class)
class SnResolverTest {

    private static final String SN = "864852062800536";
    private static final String AEP_ID = "99014977864852062800536";
    private static final String DEVICE_TYPE = "dh-wt2ga";

    @Mock
    private DeviceRegistry registry;

    private SnResolver resolver;

    @BeforeEach
    void setUp() {
        RegistryProperties properties = new RegistryProperties();
        properties.setDeviceType(DEVICE_TYPE);
        resolver = new SnResolver(registry, properties);
    }

    @Test
    void resolves_by_device_sn_first() {
        when(registry.bySn(SN)).thenReturn(Optional.of(device(SN)));

        assertEquals(SN, resolver.resolve(push(SN, AEP_ID)).map(DeviceInfo::getSn).orElse(""));
    }

    @Test
    void resolves_by_device_id_when_device_sn_absent() {
        when(registry.byAepId(AEP_ID)).thenReturn(Optional.of(device(SN)));

        assertEquals(SN, resolver.resolve(push(null, AEP_ID)).map(DeviceInfo::getSn).orElse(""));
    }

    @Test
    void returns_empty_when_both_indexes_miss() {
        assertFalse(resolver.resolve(push(SN, AEP_ID)).isPresent());
    }

    @Test
    void device_sn_with_other_type_is_rejected() {
        DeviceInfo other = device(SN);
        other.setDeviceType("dh-cms12gb");
        when(registry.bySn(SN)).thenReturn(Optional.of(other));

        assertFalse(resolver.resolve(push(SN, null)).isPresent());
    }

    private DeviceInfo device(String sn) {
        return DeviceInfo.builder().sn(sn).aepId(AEP_ID).tenantId(1L).deviceType(DEVICE_TYPE).build();
    }

    private CtwingPushMessage push(String deviceSn, String deviceId) {
        CtwingPushMessage push = new CtwingPushMessage();
        push.setDeviceSn(deviceSn);
        push.setDeviceId(deviceId);
        push.setMessageType("eventReport");
        return push;
    }
}
