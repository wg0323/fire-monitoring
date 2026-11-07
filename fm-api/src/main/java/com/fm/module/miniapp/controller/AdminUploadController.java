package com.fm.module.miniapp.controller;

import com.fm.common.api.Result;
import com.fm.module.miniapp.service.UploadService;
import com.fm.module.miniapp.vo.UploadVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 后台图片上传接口：平面图、营业执照与场所照片统一走这里。
 */
@Tag(name = "后台上传", description = "平面图、营业执照与场所照片统一上传入口")
@RestController
@RequestMapping("/api/admin/upload")
@RequiredArgsConstructor
public class AdminUploadController {

    private final UploadService uploadService;

    @Operation(summary = "上传图片", description = "校验格式与大小后保存，返回可访问 URL")
    @ApiResponse(responseCode = "200", description = "上传成功")
    @PostMapping("/image")
    public Result<UploadVO> image(
            @Parameter(description = "图片文件", required = true) @RequestParam("file") MultipartFile file) {
        return Result.ok(uploadService.saveImage(file));
    }
}
