package com.fm.module.notification.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 小程序订阅模板配置。
 */
@Data
@Schema(description = "小程序订阅模板配置")
public class SubscribeConfigVO {

    @Schema(description = "报警template主键ID")
    private String alarmTemplateId;
    @Schema(description = "隐患template主键ID")
    private String dangerTemplateId;
    @Schema(description = "是否启用")
    private Boolean enabled;
}
