package com.fm.module.miniapp.service.impl;

import com.fm.common.context.TenantContext;
import com.fm.common.exception.BizException;
import com.fm.module.miniapp.converter.MiniappConverter;
import com.fm.module.miniapp.service.ImageUploadSupport;
import com.fm.module.miniapp.service.UploadService;
import com.fm.module.miniapp.vo.UploadVO;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.SetBucketPolicyArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * MinIO 图片上传：项目唯一的图片存储实现。
 * 启动时确保 bucket 存在，并设置为公共只读，返回可直接访问的对象 URL。
 */
@Slf4j
@Service
public class MinioUploadServiceImpl implements UploadService {

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final MinioClient client;
    private final String bucket;
    private final String publicUrl;
    private volatile boolean ready;

    public MinioUploadServiceImpl(@Value("${fm.minio.endpoint}") String endpoint,
                                  @Value("${fm.minio.access-key}") String accessKey,
                                  @Value("${fm.minio.secret-key}") String secretKey,
                                  @Value("${fm.minio.bucket}") String bucket,
                                  @Value("${fm.minio.public-url:}") String publicUrl) {
        this.client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        this.bucket = bucket;
        String base = StringUtils.hasText(publicUrl) ? publicUrl : stripSlash(endpoint) + "/" + bucket;
        this.publicUrl = stripSlash(base);
    }

    @PostConstruct
    public void init() {
        tryInit();
    }

    @Override
    public UploadVO saveImage(MultipartFile file) {
        if (!ready) {
            tryInit();
        }
        if (!ready) {
            throw new BizException(503, "图片存储不可用，请联系管理员");
        }
        String extension = ImageUploadSupport.validateAndExtension(file);
        String contentType = ImageUploadSupport.contentType(extension);
        // 无登录态（MQ 等）时 TenantContext.getTenantId() 返回 0L，对象统一落到 0/ 前缀
        String objectName = TenantContext.getTenantId() + "/"
                + LocalDate.now().format(DAY_FORMAT) + "/"
                + UUID.randomUUID().toString().replace("-", "") + "." + extension;
        try (InputStream input = file.getInputStream()) {
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(input, file.getSize(), -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception ex) {
            log.error("minio put object failed, bucket={}, object={}", bucket, objectName, ex);
            throw new BizException("图片保存失败");
        }
        return MiniappConverter.toUploadVO(publicUrl + "/" + objectName);
    }

    private synchronized void tryInit() {
        if (ready) {
            return;
        }
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("minio bucket created: {}", bucket);
            }
            client.setBucketPolicy(SetBucketPolicyArgs.builder()
                    .bucket(bucket)
                    .config(readOnlyPolicy())
                    .build());
            ready = true;
            log.info("minio upload enabled, bucket={}, publicUrl={}", bucket, publicUrl);
        } catch (Exception ex) {
            log.error("MinIO 初始化失败，图片上传暂不可用，请检查 endpoint/凭据配置", ex);
        }
    }

    /**
     * 保持 bucket/* 公共只读：存量对象位于「日期/UUID」前缀且 URL 已落库，
     * 收窄到 public/* 会让所有历史图片 403，属破坏性变更，故本次不改（bucket 私有化另排期）。
     */
    private String readOnlyPolicy() {
        return "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\","
                + "\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:GetObject\"],"
                + "\"Resource\":[\"arn:aws:s3:::" + bucket + "/*\"]}]}";
    }

    private String stripSlash(String value) {
        String result = value == null ? "" : value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
