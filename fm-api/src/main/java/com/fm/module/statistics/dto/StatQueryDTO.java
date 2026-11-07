package com.fm.module.statistics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 统计分析查询：时间区间与明细分页。
 */
@Schema(description = "统计分析查询")
@Data
public class StatQueryDTO {

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "页码，从 1 开始")
    private Long pageNum = 1L;
    @Schema(description = "每页条数")
    private Long pageSize = 20L;
}
