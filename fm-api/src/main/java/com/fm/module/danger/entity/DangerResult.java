package com.fm.module.danger.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 隐患整改与审核记录：每次整改插入一条，审核结果回写当次记录。
 */
@Data
@TableName("b_danger_result")
public class DangerResult {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long dangerId;
    private Long dealUserId;
    private String dealUserName;
    private String dealBy;
    private LocalDateTime dealTime;
    private String remark;
    private String pic1;
    private String pic2;
    private String pic3;
    private Long auditUserId;
    private String auditUserName;
    private Integer auditResult;
    private String auditRemark;
    private String auditPic1;
    private String auditPic2;
    private String auditPic3;
    private String checkBy;
    private LocalDateTime checkTime;
    private Integer checkResult;
    private String checkRemark;
    private String checkPic1;
    private String checkPic2;
    private String checkPic3;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
