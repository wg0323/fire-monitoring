package com.fm.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户-商户关联：一个账号可管理多家门店。
 */
@Data
@TableName("sys_user_merchant")
public class SysUserMerchant {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long userId;
    private Long merchantId;
    private Integer isDefault;
}
