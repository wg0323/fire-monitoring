package com.fm.module.notification.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 外呼配置展示：不返回 apiSecret，只标记是否已配置。
 */
@Schema(description = "外呼配置展示")
@Data
public class CallConfigVO {

    @Schema(description = "是否已配置")
    private Boolean configured;
    @Schema(description = "账号 ID")
    private String accountId;
    @Schema(description = "接口地址")
    private String host;
    @Schema(description = "外呼分机号")
    private String fromExten;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "是否为模拟外呼模式")
    private Boolean mockMode;
}
