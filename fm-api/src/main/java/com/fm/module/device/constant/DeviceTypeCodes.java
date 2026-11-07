package com.fm.module.device.constant;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 档案类设备类型编码：当前按字典 code 硬编码，后续改为 dict_device_type.category。
 */
public final class DeviceTypeCodes {

    public static final String WATER_PRESSURE = "dh-wsm20gb";
    public static final String WATER_LEVEL = "dh-level";
    public static final String ELECTRIC = "dh-elec";

    public static final Set<String> WATER_TYPES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(WATER_PRESSURE, WATER_LEVEL)));

    private DeviceTypeCodes() {
    }

    public static boolean isWater(String type) {
        return type != null && WATER_TYPES.contains(type);
    }

    public static boolean isElectric(String type) {
        return ELECTRIC.equals(type);
    }
}
