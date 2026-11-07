package com.fm.module.system.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 部门树节点。
 */
@Data
@Schema(description = "部门树节点")
public class DeptVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键ID")
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "租户主键ID")
    private Long tenantId;
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "父级主键ID")
    private Long parentId;
    @Schema(description = "名称")
    private String name;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "子部门列表")
    private List<DeptVO> children = new ArrayList<>();
}
