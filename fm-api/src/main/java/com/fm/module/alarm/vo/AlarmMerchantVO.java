package com.fm.module.alarm.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 火警处置左侧商户列表项。
 */
@Data
@Schema(description = "火警处置左侧商户列表项")
public class AlarmMerchantVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "名称")
    private String name;
    @Schema(description = "地址")
    private String addr;
    @Schema(description = "状态")
    private Integer status;
    /** 待处置报警数：默认统计未处理，火警处置台统计未核警 */
    @Schema(description = "待处置报警数：默认统计未处理，火警处置台统计未核警")
    private Long pendingCount;
    /** 待处置报警中的最高级别：1 火警 / 3 故障，无报警为 null */
    @Schema(description = "待处置报警中的最高级别：1 火警 / 3 故障，无报警为 null")
    private Integer maxLevel;
}
