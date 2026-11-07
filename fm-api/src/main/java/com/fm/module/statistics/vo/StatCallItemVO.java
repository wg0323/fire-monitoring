package com.fm.module.statistics.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通话分析明细：关联报警的商户、类型、核警状态与录音地址。
 */
@Data
@Schema(description = "通话分析明细：关联报警的商户、类型、核警状态与录音地址")
public class StatCallItemVO {

    /** 主键ID */
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "商户名称")
    private String merchantName;
    @Schema(description = "报警类型名称")
    private String alarmTypeName;
    @Schema(description = "报警时间")
    private LocalDateTime alarmTime;
    @Schema(description = "verify状态")
    private Integer verifyStatus;
    @Schema(description = "外呼开始")
    private LocalDateTime callBegin;
    @Schema(description = "called否")
    private String calledNo;
    @Schema(description = "文件链接地址")
    private String fileUrl;
}
