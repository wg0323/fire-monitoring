package com.fm.module.alarm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报警列表查询条件。
 */
@Schema(description = "报警列表查询条件")
@Data
public class AlarmQueryDTO {

    @Schema(description = "页码，从 1 开始")
    private Integer pageNum = 1;
    @Schema(description = "每页条数")
    private Integer pageSize = 10;
    @Schema(description = "企业 ID")
    private Long merchantId;
    @Schema(description = "设备 SN")
    private String sn;
    /** 设备编号 / 位置模糊搜索 */
    @Schema(description = "设备编号或位置模糊搜索关键字")
    private String keyword;
    @Schema(description = "报警类型")
    private String alarmType;
    @Schema(description = "报警级别")
    private Integer level;
    @Schema(description = "处理状态")
    private Integer status;
    @Schema(description = "处理结果")
    private Integer result;
    @Schema(description = "复核状态")
    private Integer verifyStatus;
    @Schema(description = "是否排除已核警记录，true 时仅返回待核警与核警中的报警")
    private Boolean excludeVerified;
    @Schema(description = "开始时间")
    private LocalDateTime startTime;
    @Schema(description = "结束时间")
    private LocalDateTime endTime;
}
