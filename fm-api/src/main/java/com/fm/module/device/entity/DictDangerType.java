package com.fm.module.device.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 字典：隐患类型。
 */
@Data
@TableName("dict_danger_type")
public class DictDangerType {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /** 租户 ID：0=平台基线 */
    private Long tenantId;
    private String code;
    private String name;
    private String remark;
    private Integer status;
}
