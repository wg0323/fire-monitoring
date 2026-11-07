package com.fm.module.inspection.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 巡查计划：按工作区创建，计划只覆盖该工作区商户。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("b_plan")
public class Plan extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long tenantId;
    private Long workAreaId;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private String remark;
}
