package com.fm.module.danger.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 隐患处理时间线。
 */
@Data
@TableName("b_danger_trace")
public class DangerTrace {

    public static final int TYPE_REPORT = 0;
    public static final int TYPE_ASSIGN = 1;
    public static final int TYPE_DEAL = 2;
    public static final int TYPE_CHECK = 3;
    public static final int TYPE_CONVERT = 4;
    public static final int TYPE_AUDIT = 5;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long dangerId;
    private Integer traceType;
    private Integer result;
    private String content;
    private String detail;
    private LocalDateTime createTime;
}
