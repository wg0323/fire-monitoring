package com.fm.module.monitor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 电气/水压列表查询。
 */
@Schema(description = "电气/水压列表查询")
@Data
public class MonitorQueryDTO {

    @Schema(description = "页码，从 1 开始")
    private long pageNum = 1;
    @Schema(description = "每页条数")
    private long pageSize = 10;
    @Schema(description = "模糊搜索关键字")
    private String keyword;
}
