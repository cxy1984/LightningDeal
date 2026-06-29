package com.lightningdeal.file.service;

import com.lightningdeal.common.exception.BizException;
import io.minio.*;
import io.minio.http.Method;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 文件存储服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    @PostConstruct
    public void init() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("MinIO 存储桶创建成功: {}", bucket);
            }
        } catch (Exception e) {
            log.warn("MinIO 初始化失败（可忽略，部署时解决）: {}", e.getMessage());
        }
    }

    /**
     * 上传文件
     */
    public String upload(MultipartFile file) {
        String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(filename)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            log.info("文件上传成功: {}", filename);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new BizException(500, "文件上传失败");
        }
        return getFileUrl(filename);
    }

    /**
     * 获取文件访问 URL
     */
    public String getFileUrl(String filename) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(bucket)
                    .object(filename)
                    .method(Method.GET)
                    .expiry(7, TimeUnit.DAYS)
                    .build());
        } catch (Exception e) {
            log.error("获取文件URL失败", e);
            return null;
        }
    }

    /**
     * 删除文件
     */
    public void delete(String filename) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(filename)
                    .build());
            log.info("文件删除成功: {}", filename);
        } catch (Exception e) {
            log.error("文件删除失败", e);
        }
    }
}
