package com.actrade.activationrunner.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    @NotBlank
    private String endpoint = "http://localhost:9000";

    @NotBlank
    private String accessKey = "minioadmin";

    @NotBlank
    private String secretKey = "minioadmin";

    /** 存储执行产物的 bucket */
    @NotBlank
    private String bucketResults = "activation-results";

    /** 存储执行产物的 bucket */
    @NotBlank
    private String bucketArtifacts = "activation-artifacts";

    /** 下载超时时间（秒） */
    @Positive
    private int downloadTimeoutSeconds = 300;

    /** 下载缓冲区大小 */
    @Positive
    private int bufferSize = 8192;

    /** 是否启用校验 */
    private boolean checksumEnabled = true;

    /** 连接池大小 */
    @Positive
    private int connectionPoolSize = 10;

    /** 上传缓冲区大小 */
    @Positive
    private int uploadBufferSize = 8192;

    /** 上传超时时间（秒） */
    @Positive
    private int uploadTimeoutSeconds = 300;

    /** 自动创建 bucket */
    private boolean autoCreateBucket = true;
}