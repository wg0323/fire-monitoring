package com.fm.module.alarm.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 下行指令任务展示对象。
 */
@Data
@Schema(description = "下行指令任务展示对象")
public class CommandTaskVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "设备编号")
    private String sn;
    @Schema(description = "command类型")
    private String commandType;
    @Schema(description = "状态")
    private String status;
    @Schema(description = "req时间")
    private LocalDateTime reqTime;
    @Schema(description = "resp时间")
    private LocalDateTime respTime;
    @Schema(description = "结果")
    private String result;
    @Schema(description = "retry数量")
    private Integer retryCount;
}
