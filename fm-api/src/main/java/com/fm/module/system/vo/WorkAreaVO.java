package com.fm.module.system.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工作区展示对象。
 */
@Data
@Schema(description = "工作区展示对象")
public class WorkAreaVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键ID")
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "租户主键ID")
    private Long tenantId;
    @Schema(description = "租户名称")
    private String tenantName;
    @Schema(description = "名称")
    private String name;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "用户数量")
    private Long userCount;
    @Schema(description = "商户数量")
    private Long merchantCount;
    @JsonSerialize(contentUsing = ToStringSerializer.class)
    @Schema(description = "用户主键ID列表")
    private List<Long> userIds;
    @JsonSerialize(contentUsing = ToStringSerializer.class)
    @Schema(description = "商户主键ID列表")
    private List<Long> merchantIds;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
