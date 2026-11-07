package com.fm.module.alarm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 报警可外呼联系人：业主与第一责任人等商户档案电话。
 */
@Schema(description = "报警可外呼联系人")
@Data
public class AlarmContactVO {

    @Schema(description = "联系人身份：业主 / 第一责任人 / 第二联系人")
    private String relation;

    @Schema(description = "联系人姓名")
    private String name;

    @Schema(description = "联系电话")
    private String phone;
}
