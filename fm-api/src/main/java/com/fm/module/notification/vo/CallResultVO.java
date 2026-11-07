package com.fm.module.notification.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 外呼结果。
 */
@Data
@Schema(description = "外呼结果")
public class CallResultVO {

    @Schema(description = "字段 succeed")
    private Boolean succeed;
    @Schema(description = "消息")
    private String message;
    @Schema(description = "action主键ID")
    private String actionId;
}
