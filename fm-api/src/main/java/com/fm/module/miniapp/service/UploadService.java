package com.fm.module.miniapp.service;

import com.fm.module.miniapp.vo.UploadVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图片上传：统一存入 MinIO。
 */
public interface UploadService {

    UploadVO saveImage(MultipartFile file);
}
