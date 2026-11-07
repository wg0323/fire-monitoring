package com.fm.module.statistics.vo;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 通话分析：录音记录分页。
 */
@Data
@Schema(description = "通话分析：录音记录分页")
public class StatCallVO {

    @Schema(description = "分页")
    private Page<StatCallItemVO> page;
}
