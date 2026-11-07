package com.fm.module.miniapp.service;

import com.fm.common.exception.BizException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 图片上传校验：扩展名白名单 + 文件魔数 + 固定 Content-Type 映射。
 * 不再信任客户端传入的 contentType，避免 SVG 等伪造类型造成存储型 XSS。
 */
public final class ImageUploadSupport {

    private static final long MAX_SIZE = 5L * 1024 * 1024;
    private static final int MAGIC_BUFFER_LENGTH = 12;
    private static final Map<String, String> ALLOWED_TYPES = allowedTypes();
    private static final byte[] MAGIC_PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] MAGIC_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] MAGIC_GIF = {0x47, 0x49, 0x46, 0x38};
    private static final byte[] MAGIC_BMP = {0x42, 0x4D};
    private static final byte[] MAGIC_RIFF = {0x52, 0x49, 0x46, 0x46};
    private static final byte[] MAGIC_WEBP = {0x57, 0x45, 0x42, 0x50};

    private ImageUploadSupport() {
    }

    /**
     * 校验图片并返回白名单扩展名；扩展名与文件头必须一致。
     */
    public static String validateAndExtension(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择图片");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BizException("图片不能超过 5MB");
        }
        String extension = resolveExtension(file.getOriginalFilename());
        if ("svg".equals(extension) || "svgz".equals(extension)) {
            throw new BizException("不允许上传 SVG 图片");
        }
        if (!ALLOWED_TYPES.containsKey(extension)) {
            throw new BizException("不支持的图片格式");
        }
        validateMagic(file, extension);
        return extension;
    }

    /**
     * 按白名单扩展名返回固定的 Content-Type，禁止回写客户端传入的 contentType。
     */
    public static String contentType(String extension) {
        String type = ALLOWED_TYPES.get(extension == null ? "" : extension.toLowerCase(Locale.ROOT));
        if (type == null) {
            throw new BizException("不支持的图片格式");
        }
        return type;
    }

    private static Map<String, String> allowedTypes() {
        Map<String, String> types = new HashMap<>(8);
        types.put("jpg", "image/jpeg");
        types.put("jpeg", "image/jpeg");
        types.put("png", "image/png");
        types.put("webp", "image/webp");
        types.put("gif", "image/gif");
        types.put("bmp", "image/bmp");
        return Collections.unmodifiableMap(types);
    }

    /** 扩展名只取原始文件名，文件名缺失或无扩展名时返回空串交由白名单拒绝。 */
    private static String resolveExtension(String originalName) {
        if (originalName == null) {
            return "";
        }
        int dot = originalName.lastIndexOf('.');
        if (dot < 0 || dot == originalName.length() - 1) {
            return "";
        }
        return originalName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static void validateMagic(MultipartFile file, String extension) {
        byte[] head = new byte[MAGIC_BUFFER_LENGTH];
        int length;
        try (InputStream input = file.getInputStream()) {
            length = input.read(head);
        } catch (IOException ex) {
            throw new BizException("图片读取失败");
        }
        if (length < 0 || !matchesMagic(head, length, extension)) {
            throw new BizException("图片内容与格式不符");
        }
    }

    private static boolean matchesMagic(byte[] head, int length, String extension) {
        if ("png".equals(extension)) {
            return startsWith(head, length, MAGIC_PNG);
        }
        if ("jpg".equals(extension) || "jpeg".equals(extension)) {
            return startsWith(head, length, MAGIC_JPEG);
        }
        if ("gif".equals(extension)) {
            return startsWith(head, length, MAGIC_GIF);
        }
        if ("bmp".equals(extension)) {
            return startsWith(head, length, MAGIC_BMP);
        }
        if ("webp".equals(extension)) {
            return length >= MAGIC_BUFFER_LENGTH && startsWith(head, length, MAGIC_RIFF)
                    && head[8] == MAGIC_WEBP[0] && head[9] == MAGIC_WEBP[1]
                    && head[10] == MAGIC_WEBP[2] && head[11] == MAGIC_WEBP[3];
        }
        return false;
    }

    private static boolean startsWith(byte[] head, int length, byte[] magic) {
        if (length < magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if (head[i] != magic[i]) {
                return false;
            }
        }
        return true;
    }
}
