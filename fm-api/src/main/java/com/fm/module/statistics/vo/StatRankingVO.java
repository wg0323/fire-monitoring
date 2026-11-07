package com.fm.module.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统计排行项：隐患类型、故障商户、通话商户。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "统计排行项：隐患类型、故障商户、通话商户")
public class StatRankingVO {

    @Schema(description = "名称")
    private String name;
    @Schema(description = "数值")
    private Long value;
}
