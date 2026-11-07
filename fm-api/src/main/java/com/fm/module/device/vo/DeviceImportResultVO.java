package com.fm.module.device.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 设备导入结果：成功行已入库，失败行返回原因，单次请求内部分成功。
 */
@Data
@Schema(description = "设备导入结果：成功行已入库，失败行返回原因，单次请求内部分成功")
public class DeviceImportResultVO {

    @Schema(description = "总数")
    private int total;
    @Schema(description = "成功")
    private int success;
    @Schema(description = "失败")
    private int failed;
    @Schema(description = "失败行明细")
    private List<DeviceImportErrorVO> errors = new ArrayList<>();
}
