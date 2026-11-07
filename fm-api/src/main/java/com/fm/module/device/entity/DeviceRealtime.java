package com.fm.module.device.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备最后上报快照，供列表/大屏展示。
 */
@Data
@TableName("b_device_realtime")
public class DeviceRealtime {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private String sn;
    private String type;
    private String data;
    private LocalDateTime reportTime;
    private LocalDateTime updateTime;
}
