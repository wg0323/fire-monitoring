package com.fm.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 部门：租户内组织单元，可为空。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dept")
public class SysDept extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long tenantId;
    private Long parentId;
    private String name;
    private Integer status;
}
