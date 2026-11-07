package com.fm.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 组织：新模型下只保留 TENANT（运营中心即租户）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_org")
public class SysOrg extends BaseEntity {

    private static final long serialVersionUID = 1L;

    public static final String TYPE_TENANT = "TENANT";

    private String type;
    private Long parentId;
    private String name;
    private String code;
    private String adcode;
    private String address;
    private Integer status;
}
