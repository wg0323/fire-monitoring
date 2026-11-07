package com.fm.module.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 水压/液位历史数据。
 */
@Data
@TableName("b_water_history")
public class WaterHistory {

    @TableId(value = "f_id", type = IdType.AUTO)
    private Long id;
    @TableField("f_sn")
    private String sn;
    @TableField("f_measure_value")
    private Integer measureValue;
    @TableField("f_measure_type")
    private Integer measureType;
    @TableField("f_measure_time")
    private LocalDateTime measureTime;
    @TableField("f_create_time")
    private LocalDateTime createTime;
}
