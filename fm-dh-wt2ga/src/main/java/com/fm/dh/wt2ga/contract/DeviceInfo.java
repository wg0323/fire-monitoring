package com.fm.dh.wt2ga.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 注册表条目：采集器只依赖业务 sn、平台设备 id、租户与设备类型。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceInfo {

    private String sn;
    private String aepId;
    private Long tenantId;
    private String deviceType;
}
