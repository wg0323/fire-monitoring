package com.fm.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 工作区-商户关联。
 */
@Data
@TableName("sys_work_area_merchant")
public class SysWorkAreaMerchant {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long workAreaId;
    private Long merchantId;
}
