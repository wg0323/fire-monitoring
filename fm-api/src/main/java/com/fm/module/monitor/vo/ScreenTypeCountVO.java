package com.fm.module.monitor.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 大屏排行 / 占比项。
 */
@Data
@Schema(description = "大屏排行 / 占比项")
public class ScreenTypeCountVO {

    @Schema(description = "名称")
    private String name;
    @Schema(description = "数值")
    private Long value;

    public ScreenTypeCountVO() {
    }

    public ScreenTypeCountVO(String name, Long value) {
        this.name = name;
        this.value = value;
    }
}
