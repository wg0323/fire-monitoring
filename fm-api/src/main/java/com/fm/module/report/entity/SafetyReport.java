package com.fm.module.report.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 安全报告：月报/年报文件记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("b_safety_report")
public class SafetyReport extends BaseEntity {

    private static final long serialVersionUID = 1L;

    public static final int TYPE_MONTH = 0;
    public static final int TYPE_YEAR = 1;

    private Long tenantId;
    private Integer type;
    private String reportName;
    private String filePath;
    private LocalDate reportTime;
}
