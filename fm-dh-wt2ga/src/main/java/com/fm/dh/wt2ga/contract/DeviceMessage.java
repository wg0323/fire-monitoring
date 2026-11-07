package com.fm.dh.wt2ga.contract;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一消息信封，字段与 fm-api 的 DeviceMessage 保持一致。
 */
@Data
public class DeviceMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String messageId;
    private String sn;
    private Long tenantId;
    private String type;
    private Long eventTime;
    private Integer version;
    private JsonNode payload;
}
