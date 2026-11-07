package com.fm.module.inspection.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 巡查结果：一条记录对应一个商户的一次巡查。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("b_plan_result")
public class PlanResult extends BaseEntity {

    private static final long serialVersionUID = 1L;

    public static final int RESULT_NORMAL = 0;
    public static final int RESULT_DANGER = 1;

    private Long tenantId;
    private Long planId;
    private Long merchantId;
    private Long userId;
    private Long workAreaId;
    private Long dangerId;
    private String patrolBy;
    private LocalDateTime patrolTime;
    private BigDecimal lng;
    private BigDecimal lat;
    private Integer result;
    private String remark;
    private String pic1;
    private String pic2;
    private String pic3;
}
