package com.fm.module.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 小程序订阅消息授权记录。
 */
@Schema(description = "小程序订阅消息授权记录")
@Data
public class SubscribeDTO {

    @NotEmpty
    @Schema(description = "订阅消息模板 ID 列表")
    private List<String> templateIds;
}
