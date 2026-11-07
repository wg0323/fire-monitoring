package com.fm.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 运营中心（租户）新增/修改请求。
 */
@Schema(description = "运营中心（租户）新增/修改请求")
@Data
public class OrgSaveDTO {

    @Schema(description = "主键 ID")
    private Long id;
    @Schema(description = "名称")
    private String name;
    @Schema(description = "编码")
    private String code;
    @Schema(description = "行政区划代码")
    private String adcode;
    @Schema(description = "地址")
    private String address;
    @Schema(description = "状态")
    private Integer status;
}
