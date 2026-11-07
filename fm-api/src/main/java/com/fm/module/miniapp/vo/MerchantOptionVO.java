package com.fm.module.miniapp.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 小程序商户选择项：随手拍/上报时使用。
 */
@Data
@Schema(description = "小程序商户选择项：随手拍/上报时使用")
public class MerchantOptionVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "名称")
    private String name;
    @Schema(description = "地址")
    private String addr;
}
