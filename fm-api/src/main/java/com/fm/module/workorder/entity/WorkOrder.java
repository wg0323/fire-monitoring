package com.fm.module.workorder.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 工单：报警派单自动生成，运营人员接单并完成。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("b_work_order")
public class WorkOrder extends BaseEntity {

    private static final long serialVersionUID = 1L;

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_PROCESSING = 1;
    public static final int STATUS_FINISHED = 2;

    public static final String SOURCE_DISPATCH = "DISPATCH";

    private Long tenantId;
    private Long merchantId;
    private Long alarmId;
    private Long workAreaId;
    private String source;
    private String title;
    private String content;
    private Long handlerId;
    private String handlerName;
    private Integer status;
    private Integer priority;
    private LocalDateTime acceptTime;
    private LocalDateTime finishTime;
    private String finishRemark;
    private String finishPic1;
    private String finishPic2;
    private String finishPic3;
}
