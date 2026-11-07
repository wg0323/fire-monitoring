package com.fm.module.statistics.vo;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 报警分析 / 真警分析：趋势与分页明细。
 */
@Data
@Schema(description = "报警分析 / 真警分析：趋势与分页明细")
public class StatAlarmVO {

    @Schema(description = "趋势")
    private List<StatTrendPointVO> trend;
    @Schema(description = "分页")
    private Page<StatAlarmItemVO> page;
}
