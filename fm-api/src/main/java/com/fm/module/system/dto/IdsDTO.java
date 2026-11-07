package com.fm.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 批量绑定/授权请求。
 */
@Schema(description = "批量绑定/授权请求")
@Data
public class IdsDTO {

    @Schema(description = "ID 列表")
    private List<Long> ids;
}
