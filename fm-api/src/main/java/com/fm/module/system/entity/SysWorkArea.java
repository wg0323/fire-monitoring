package com.fm.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工作区：只用于派单选人反查，不参与行级数据权限。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_work_area")
public class SysWorkArea extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long tenantId;
    private Long orgId;
    private String name;
    private String remark;
}
