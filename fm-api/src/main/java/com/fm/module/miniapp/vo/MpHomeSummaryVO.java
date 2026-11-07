package com.fm.module.miniapp.vo;

import com.fm.module.alarm.vo.AlarmVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 小程序首页汇总：2x2 KPI + 最新报警 / 隐患。
 */
@Data
@Schema(description = "小程序首页汇总：2x2 KPI + 最新报警 / 隐患")
public class MpHomeSummaryVO {

    @Schema(description = "报警数量")
    private Long alarmCount;
    @Schema(description = "隐患数量")
    private Long dangerCount;
    @Schema(description = "设备数量")
    private Long deviceCount;
    @Schema(description = "离线数量")
    private Long offlineCount;
    /** 待处理工单数：待接单 + 处理中。 */
    @Schema(description = "待处理工单数：待接单 + 处理中")
    private Long workOrderCount;
    /** 进行中且仍有商户未巡查的计划数。 */
    @Schema(description = "进行中且仍有商户未巡查的计划数")
    private Long inspectionCount;
    @Schema(description = "最新报警")
    private List<AlarmVO> latestAlarms;
    @Schema(description = "最新隐患")
    private List<DangerBrief> latestDangers;

    /**
     * 首页隐患摘要。
     */
    @Data
    @Schema(description = "首页隐患摘要")
    public static class DangerBrief {
        @Schema(description = "主键ID")
        private String id;
        @Schema(description = "备注")
        private String remark;
        @Schema(description = "商户名称")
        private String merchantName;
        @Schema(description = "状态")
        private Integer status;
        @Schema(description = "来源类型")
        private Integer sourceType;
        @Schema(description = "创建时间")
        private LocalDateTime createTime;
    }
}
