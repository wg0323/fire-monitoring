package com.fm.module.miniapp.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 上传结果：返回可直接访问的相对路径。
 */
@Data
@Schema(description = "上传结果：返回可直接访问的相对路径")
public class UploadVO {

    @Schema(description = "链接地址")
    private String url;
}
