package com.fm.module.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 监控大屏首页统计：字段与旧监控后台的 monitorHome 接口保持一致。
 */
@Data
@Schema(description = "监控大屏首页统计：字段与旧监控后台的 monitorHome 接口保持一致")
public class MonitorHomeVO {

    /** 今日火警 */
    @Schema(description = "今日火警")
    private Long fireAlarmNum;
    /** 今日已处理火警 */
    @Schema(description = "今日已处理火警")
    private Long dealFireAlarmNum;
    /** 今日核警为真警 */
    @Schema(description = "今日核警为真警")
    private Long realFireAlarmNum;
    /** 未处理隐患 */
    @Schema(description = "未处理隐患")
    private Long noDealDangerNum;
    /** 今日新增隐患 */
    @Schema(description = "今日新增隐患")
    private Long addDangerNum;
    /** 逾期未处置隐患 */
    @Schema(description = "逾期未处置隐患")
    private Long expectDangerNum;
    /** 今日巡查企业数 */
    @Schema(description = "今日巡查企业数")
    private Long patrolNum;
    /** 巡查占比（%） */
    @Schema(description = "巡查占比（%）")
    private Double patrolPercent;
    /** 接入企业数 */
    @Schema(description = "接入企业数")
    private Long nspNum;
    /** 设备总数 */
    @Schema(description = "设备总数")
    private Long totalDeviceNum;
    /** 正常运行设备数 */
    @Schema(description = "正常运行设备数")
    private Long normalDeviceNum;
    /** 故障设备数（新库暂无故障状态，固定为 0） */
    @Schema(description = "故障设备数（新库暂无故障状态，固定为 0）")
    private Long faultDeviceNum;
    /** 报警设备数（新库暂无设备级报警快照，固定为 0） */
    @Schema(description = "报警设备数（新库暂无设备级报警快照，固定为 0）")
    private Long alarmDeviceNum;
    /** 离线设备数 */
    @Schema(description = "离线设备数")
    private Long offlineDeviceNum;
}
