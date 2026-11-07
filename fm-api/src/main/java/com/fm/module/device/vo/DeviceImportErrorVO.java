package com.fm.module.device.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 导入失败行。
 */
@Data
@Schema(description = "导入失败行")
public class DeviceImportErrorVO {

    @Schema(description = "行数据数量")
    private int rowNum;
    @Schema(description = "设备编号")
    private String sn;
    @Schema(description = "消息")
    private String message;
}
