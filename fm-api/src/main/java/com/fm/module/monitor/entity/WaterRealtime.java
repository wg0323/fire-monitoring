package com.fm.module.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 水压/液位实时数据（按 SN 唯一）。
 */
@Data
@TableName("b_water_realtime")
public class WaterRealtime {

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
    @TableField("f_update_time")
    private LocalDateTime updateTime;
}
