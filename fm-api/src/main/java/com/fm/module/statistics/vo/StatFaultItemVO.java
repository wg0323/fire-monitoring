package com.fm.module.statistics.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 故障分析明细：设备编号、来源、上报时间、报警描述与处置状态。
 */
@Data
@Schema(description = "故障分析明细：设备编号、来源、上报时间、报警描述与处置状态")
public class StatFaultItemVO {

    /** 主键ID */
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "设备编号")
    private String sn;
    @Schema(description = "商户名称")
    private String merchantName;
    @Schema(description = "位置")
    private String position;
    @Schema(description = "报警时间")
    private LocalDateTime alarmTime;
    @Schema(description = "报警类型名称")
    private String alarmTypeName;
    @Schema(description = "处理来源")
    private String handleSource;
    @Schema(description = "结果")
    private Integer result;
    @Schema(description = "状态")
    private Integer status;
}
