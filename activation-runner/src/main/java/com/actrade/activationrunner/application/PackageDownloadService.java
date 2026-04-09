package com.actrade.activationrunner.application;

import com.actrade.activationrunner.client.dto.PackageAssetDto;
import com.actrade.activationrunner.config.MinioProperties;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * 工具包下载服务
 * 从 MinIO 下载激活工具包到本地工作空间
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PackageDownloadService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final ChecksumValidator checksumValidator;
    private final ArchiveExtractor archiveExtractor;

    /**
     * 下载并解压工具包
     *
     * @param packageAsset 工具包资产信息
     * @param targetDir    目标目录（工具包将被解压到此目录）
     * @return 下载结果
     */
    public PackageDownloadResult downloadAndExtract(
            @NotNull PackageAssetDto packageAsset,
            @NotNull Path targetDir
    ) {
        if (packageAsset == null) {
            return PackageDownloadResult.failure("Package asset is null");
        }

        String bucketName = packageAsset.bucketName();
        String objectKey = packageAsset.objectKey();

        if (!StringUtils.hasText(bucketName)) {
            return PackageDownloadResult.failure("Bucket name is empty");
        }

        if (!StringUtils.hasText(objectKey)) {
            return PackageDownloadResult.failure("Object key is empty");
        }

        log.info("Starting package download. bucket={}, objectKey={}, fileId={}",
                bucketName, objectKey, packageAsset.fileId());

        Path downloadedFile = null;
        long fileSizeBytes = 0;

        try {
            // 1. 创建临时下载目录
            Path tempDir = targetDir.resolve("temp_download");
            Files.createDirectories(tempDir);

            // 2. 确定下载文件名
            String filename = StringUtils.hasText(packageAsset.storedFilename())
                    ? packageAsset.storedFilename()
                    : extractFilename(objectKey);
            downloadedFile = tempDir.resolve(filename);

            // 3. 下载文件到本地
            fileSizeBytes = downloadFile(bucketName, objectKey, downloadedFile);

            // 4. 校验 SHA-256
            if (StringUtils.hasText(packageAsset.checksumSha256())) {
                ChecksumValidator.ValidationResult validationResult =
                        checksumValidator.validate(downloadedFile, packageAsset.checksumSha256());

                if (!validationResult.valid()) {
                    // 删除无效文件
                    Files.deleteIfExists(downloadedFile);
                    return PackageDownloadResult.failure(
                            "CHECKSUM_MISMATCH",
                            "Checksum validation failed: " + validationResult.errorMessage()
                    );
                }

                log.info("Checksum validated successfully. fileId={}, checksum={}",
                        packageAsset.fileId(), packageAsset.checksumSha256());
            }

            // 5. 解压到目标目录
            Path extractedDir = targetDir.resolve("package");
            ArchiveExtractor.ExtractResult extractResult = archiveExtractor.extract(downloadedFile, extractedDir);

            if (!extractResult.success()) {
                return PackageDownloadResult.failure(
                        "EXTRACTION_FAILED",
                        "Failed to extract archive: " + extractResult.errorMessage()
                );
            }

            // 6. 删除临时压缩包
            Files.deleteIfExists(downloadedFile);

            log.info("Package downloaded and extracted successfully. " +
                            "fileId={}, extractedFiles={}, extractedDir={}",
                    packageAsset.fileId(), extractResult.fileCount(), extractedDir);

            return PackageDownloadResult.success(
                    downloadedFile,
                    extractedDir,
                    packageAsset.checksumSha256(),
                    fileSizeBytes
            );

        } catch (ErrorResponseException e) {
            log.error("MinIO error response during download. bucket={}, objectKey={}, error={}",
                    bucketName, objectKey, e.errorResponse().message(), e);
            cleanupTempFile(downloadedFile);
            return PackageDownloadResult.failure("MINIO_NOT_FOUND", "Object not found in MinIO: " + objectKey);

        } catch (MinioException e) {
            log.error("MinIO error during download. bucket={}, objectKey={}", bucketName, objectKey, e);
            cleanupTempFile(downloadedFile);
            return PackageDownloadResult.failure("MINIO_ERROR", "MinIO error: " + e.getMessage());

        } catch (IOException e) {
            log.error("IO error during download. bucket={}, objectKey={}", bucketName, objectKey, e);
            cleanupTempFile(downloadedFile);
            return PackageDownloadResult.failure("IO_ERROR", "IO error: " + e.getMessage());

        } catch (Exception e) {
            log.error("Unexpected error during download. bucket={}, objectKey={}", bucketName, objectKey, e);
            cleanupTempFile(downloadedFile);
            return PackageDownloadResult.failure("UNEXPECTED_ERROR", "Unexpected error: " + e.getMessage());
        }
    }

    /**
     * 仅下载工具包（不解压）
     *
     * @param packageAsset 工具包资产信息
     * @param targetDir    目标目录
     * @return 下载结果
     */
    public PackageDownloadResult downloadOnly(
            @NotNull PackageAssetDto packageAsset,
            @NotNull Path targetDir
    ) {
        if (packageAsset == null) {
            return PackageDownloadResult.failure("Package asset is null");
        }

        String bucketName = packageAsset.bucketName();
        String objectKey = packageAsset.objectKey();

        log.info("Downloading package without extraction. bucket={}, objectKey={}", bucketName, objectKey);

        Path downloadedFile = null;

        try {
            Path tempDir = targetDir.resolve("temp_download");
            Files.createDirectories(tempDir);

            String filename = StringUtils.hasText(packageAsset.storedFilename())
                    ? packageAsset.storedFilename()
                    : extractFilename(objectKey);
            downloadedFile = tempDir.resolve(filename);

            long fileSizeBytes = downloadFile(bucketName, objectKey, downloadedFile);

            // 校验
            if (StringUtils.hasText(packageAsset.checksumSha256())) {
                ChecksumValidator.ValidationResult validationResult =
                        checksumValidator.validate(downloadedFile, packageAsset.checksumSha256());

                if (!validationResult.valid()) {
                    Files.deleteIfExists(downloadedFile);
                    return PackageDownloadResult.failure(
                            "CHECKSUM_MISMATCH",
                            "Checksum validation failed: " + validationResult.errorMessage()
                    );
                }
            }

            return PackageDownloadResult.success(
                    downloadedFile,
                    null,
                    packageAsset.checksumSha256(),
                    fileSizeBytes
            );

        } catch (Exception e) {
            log.error("Error downloading package. bucket={}, objectKey={}", bucketName, objectKey, e);
            cleanupTempFile(downloadedFile);
            return PackageDownloadResult.failure("DOWNLOAD_FAILED", e.getMessage());
        }
    }

    /**
     * 检查 MinIO 连接是否正常
     *
     * @return true 如果连接正常
     */
    public boolean isMinioAvailable() {
        try {
            // 尝试获取默认 bucket 的统计信息（仅测试连接）
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProperties.getBucketArtifacts())
                            .object("health-check-placeholder")
                            .build()
            );
            return true;
        } catch (ErrorResponseException e) {
            // 404 是正常的，说明 bucket 存在
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                return true;
            }
            log.warn("MinIO health check failed: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("MinIO health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取 MinIO 中对象的大小
     */
    public long getObjectSize(String bucketName, String objectKey) throws Exception {
        try {
            var stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );
            return stat.size();
        } catch (ErrorResponseException e) {
            log.warn("Object not found. bucket={}, objectKey={}", bucketName, objectKey);
            throw e;
        }
    }

    private long downloadFile(String bucketName, String objectKey, Path targetFile) throws Exception {
        long totalBytes = 0;

        try (InputStream is = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectKey)
                        .build()
        )) {
            // 获取文件大小
            long fileSize = getObjectSize(bucketName, objectKey);

            try (var os = Files.newOutputStream(targetFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                long downloadedBytes = 0;

                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                    downloadedBytes += bytesRead;
                    totalBytes = downloadedBytes;

                    // 进度日志
                    if (fileSize > 0 && downloadedBytes % (10 * 1024 * 1024) < 8192) {
                        int progress = (int) ((downloadedBytes * 100) / fileSize);
                        log.debug("Downloading: {}% ({}/{} bytes)", progress, downloadedBytes, fileSize);
                    }
                }
            }
        }

        log.info("File downloaded. bucket={}, objectKey={}, size={} bytes", bucketName, objectKey, totalBytes);
        return totalBytes;
    }

    private String extractFilename(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return "package_" + UUID.randomUUID() + ".zip";
        }

        int lastSlash = objectKey.lastIndexOf('/');
        String filename = lastSlash >= 0 ? objectKey.substring(lastSlash + 1) : objectKey;

        // 如果没有扩展名，添加 .zip
        if (!filename.contains(".")) {
            filename += ".zip";
        }

        return filename;
    }

    private void cleanupTempFile(Path file) {
        if (file != null) {
            try {
                Files.deleteIfExists(file);
                log.debug("Cleaned up temp file: {}", file);
            } catch (IOException e) {
                log.warn("Failed to cleanup temp file: {}", file, e);
            }
        }
    }
}
