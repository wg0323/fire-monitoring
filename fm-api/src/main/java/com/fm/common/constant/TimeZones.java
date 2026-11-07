package com.fm.common.constant;

import java.time.ZoneId;

/**
 * 业务时区常量：设备上报的 epoch 毫秒统一按业务时区落库，避免容器默认时区为 UTC 时串日期。
 */
public final class TimeZones {

    public static final ZoneId BUSINESS = ZoneId.of("Asia/Shanghai");

    private TimeZones() {
    }
}
