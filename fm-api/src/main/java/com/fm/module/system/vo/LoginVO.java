package com.fm.module.system.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fm.module.device.vo.MerchantOptionVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 登录结果。
 */
@Data
@Schema(description = "登录结果")
public class LoginVO {

    @Schema(description = "令牌名称")
    private String tokenName;
    @Schema(description = "令牌数值")
    private String tokenValue;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "用户主键ID")
    private Long userId;
    @Schema(description = "用户名称")
    private String userName;
    @Schema(description = "真实名称")
    private String realName;
    @Schema(description = "用户类型")
    private String userType;
    @Schema(description = "数据scope")
    private String dataScope;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "当前商户主键ID")
    private Long currentMerchantId;
    @Schema(description = "商户")
    private List<MerchantOptionVO> merchants;
    @Schema(description = "角色")
    private List<String> roles;
    @Schema(description = "字段 permissions")
    private List<String> permissions;
}
