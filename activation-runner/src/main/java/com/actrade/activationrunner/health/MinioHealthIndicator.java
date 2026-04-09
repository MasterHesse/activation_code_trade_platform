package com.actrade.activationrunner.health;

import com.actrade.activationrunner.config.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * MinIO 连接健康检查指示器
 *
 * <p>检查 MinIO 服务是否可用，以及配置的 bucket 是否存在。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MinioHealthIndicator implements HealthIndicator {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Override
    public Health health() {
        try {
            // 测试 API 是否响应
            String endpoint = minioProperties.getEndpoint();
            
            // 检查 artifacts bucket
            boolean artifactsBucketExists = checkBucketExists(minioProperties.getBucketArtifacts());
            
            // 检查 results bucket
            boolean resultsBucketExists = checkBucketExists(minioProperties.getBucketResults());
            
            if (artifactsBucketExists && resultsBucketExists) {
                return Health.up()
                        .withDetail("endpoint", endpoint)
                        .withDetail("artifactsBucket", "ok")
                        .withDetail("resultsBucket", "ok")
                        .build();
            } else {
                return Health.unknown()
                        .withDetail("endpoint", endpoint)
                        .withDetail("artifactsBucket", artifactsBucketExists ? "ok" : "missing")
                        .withDetail("resultsBucket", resultsBucketExists ? "ok" : "missing")
                        .withDetail("warning", "Some buckets are missing, auto-create may be enabled")
                        .build();
            }
            
        } catch (Exception e) {
            log.warn("MinIO health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("endpoint", minioProperties.getEndpoint())
                    .build();
        }
    }
    
    private boolean checkBucketExists(String bucketName) {
        try {
            return minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build()
            );
        } catch (Exception e) {
            log.debug("Bucket {} check failed: {}", bucketName, e.getMessage());
            return false;
        }
    }
}
