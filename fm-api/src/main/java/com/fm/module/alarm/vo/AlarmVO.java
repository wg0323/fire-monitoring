package com.fm.module.alarm.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 报警列表/详情展示对象。
 */
@Data
@Schema(description = "报警列表/详情展示对象")
public class AlarmVO {

    /** 主键ID */
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "设备编号")
    private String sn;
    @Schema(description = "设备类型")
    private String deviceType;
    /** 设备类型名称，取 dict_device_type.name，列表展示用 */
    @Schema(description = "设备类型名称，取 dict_device_type.name，列表展示用")
    private String deviceTypeName;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "商户主键ID")
    private Long merchantId;
    /** 商户名称，列表展示用 */
    @Schema(description = "商户名称，列表展示用")
    private String merchantName;
    @Schema(description = "位置")
    private String position;
    @Schema(description = "报警时间")
    private LocalDateTime alarmTime;
    @Schema(description = "报警类型")
    private String alarmType;
    @Schema(description = "报警级别")
    private Integer alarmLevel;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "结果")
    private Integer result;
    @Schema(description = "verify状态")
    private Integer verifyStatus;
    @Schema(description = "snapshot链接地址")
    private String snapshotUrl;
    @Schema(description = "商户处理结果")
    private Integer merchantHandleResult;
    @Schema(description = "商户处理时间")
    private LocalDateTime merchantHandleTime;
    @Schema(description = "处理来源")
    private String handleSource;
    @Schema(description = "处理人名称")
    private String handlerName;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "工作区域主键ID")
    private Long workAreaId;
    @Schema(description = "工作区域名称")
    private String workAreaName;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "隐患主键ID")
    private Long dangerId;
    @Schema(description = "finalize时间")
    private LocalDateTime finalizeTime;
    @Schema(description = "隐患convertdeadline")
    private LocalDateTime dangerConvertDeadline;
    /** 待转隐患剩余秒数，已到期为 0，不适用为 null */
    @Schema(description = "待转隐患剩余秒数，已到期为 0，不适用为 null")
    private Long dangerRemainSeconds;
    @Schema(description = "恢复时间")
    private LocalDateTime recoverTime;
    /** 故障是否在 1 小时内自行恢复 */
    @Schema(description = "故障是否在 1 小时内自行恢复")
    private Boolean selfRecovered;
    @Schema(description = "dispatch时间")
    private LocalDateTime dispatchTime;
    @Schema(description = "dispatch备注")
    private String dispatchRemark;
    @Schema(description = "op用户名称")
    private String opUserName;
    @Schema(description = "新增备注")
    private String addRemark;
    @Schema(description = "新增pic1")
    private String addPic1;
    @Schema(description = "新增pic2")
    private String addPic2;
    @Schema(description = "新增pic3")
    private String addPic3;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "工作工单主键ID")
    private Long workOrderId;
    @Schema(description = "工作工单状态")
    private Integer workOrderStatus;
    @Schema(description = "楼层平面图地址")
    private String floorPlanUrl;
    @Schema(description = "楼层名称")
    private String floorName;
    @Schema(description = "设备在楼层图上的横向百分比坐标")
    private BigDecimal floorX;
    @Schema(description = "设备在楼层图上的纵向百分比坐标")
    private BigDecimal floorY;
    @Schema(description = "业主与第一责任人等可外呼联系人")
    private List<AlarmContactVO> contacts;
    @Schema(description = "最近一条处置追踪类型")
    private Integer lastTraceType;
    @Schema(description = "最近一条处置追踪结果：1 成功，2 失败")
    private Integer lastTraceResult;
    @Schema(description = "最近一条处置追踪内容")
    private String lastTraceContent;
    @Schema(description = "最近一条处置追踪时间")
    private LocalDateTime lastTraceTime;
    @Schema(description = "是否已电话告警：true 已告警，false 未告警")
    private Boolean phoneAlerted;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
