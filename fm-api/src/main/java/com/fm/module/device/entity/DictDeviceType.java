package com.fm.module.device.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 字典：设备类型。新设备类型只加字典行，不改设备主表 DDL。
 */
@Data
@TableName("dict_device_type")
public class DictDeviceType {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /** 租户 ID：0=平台基线 */
    private Long tenantId;
    private String code;
    private String name;
    private String remark;
    private Integer status;
    private Integer offlineThreshold;
    private String offlineUnit;
    private Integer monitorFlag;
    private Integer sort;
}
