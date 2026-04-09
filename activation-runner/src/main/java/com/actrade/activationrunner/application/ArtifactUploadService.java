package com.actrade.activationrunner.application;

import com.actrade.activationrunner.client.dto.ArtifactItemDto;
import com.actrade.activationrunner.config.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.MinioException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * 产物上传服务 - 负责收集工作空间中的执行产物并上传至 MinIO
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArtifactUploadService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    /**
     * 产物类型枚举
     */
    public enum ArtifactType {
        OUTPUT("output"),
        LOG_STDOUT("log-stdout"),
        LOG_STDERR("log-stderr"),
        RESULT_JSON("result-json");

        private final String typeName;

        ArtifactType(String typeName) {
            this.typeName = typeName;
        }

        public String getTypeName() {
            return typeName;
        }
    }

    /**
     * 产物文件扩展名到 mediaType 的映射
     */
    private static final Map<String, String> EXTENSION_TO_MEDIA_TYPE = Map.ofEntries(
            Map.entry(".json", "application/json"),
            Map.entry(".xml", "application/xml"),
            Map.entry(".yaml", "application/yaml"),
            Map.entry(".yml", "application/yaml"),
            Map.entry(".txt", "text/plain"),
            Map.entry(".log", "text/plain"),
            Map.entry(".csv", "text/csv"),
            Map.entry(".html", "text/html"),
            Map.entry(".htm", "text/html"),
            Map.entry(".png", "image/png"),
            Map.entry(".jpg", "image/jpeg"),
            Map.entry(".jpeg", "image/jpeg"),
            Map.entry(".gif", "image/gif"),
            Map.entry(".svg", "image/svg+xml"),
            Map.entry(".pdf", "application/pdf"),
            Map.entry(".zip", "application/zip"),
            Map.entry(".tar", "application/x-tar"),
            Map.entry(".gz", "application/gzip"),
            Map.entry(".bz2", "application/x-bzip2"),
            Map.entry(".js", "application/javascript"),
            Map.entry(".css", "text/css")
    );

    private static final String DEFAULT_MEDIA_TYPE = "application/octet-stream";

    /** MinIO multipart upload 最小 part size: 5MiB */
    private static final long MIN_PART_SIZE = 5 * 1024 * 1024;

    @PostConstruct
    public void init() {
        try {
            ensureBucketExists();
            log.info("ArtifactUploadService initialized. bucket={}", minioProperties.getBucketResults());
        } catch (Exception e) {
            log.warn("Failed to ensure bucket exists during initialization. bucket={}, error={}",
                    minioProperties.getBucketResults(), e.getMessage());
        }
    }

    /**
     * 收集并上传工作空间中的所有执行产物
     *
     * @param workspace  工作空间上下文
     * @param taskNo     任务编号
     * @param attemptNo  尝试编号
     * @return 上传结果
     */
    public ArtifactUploadResult collectAndUploadArtifacts(
            WorkspaceManager.WorkspaceContext workspace,
            String taskNo,
            Integer attemptNo) {

        if (workspace == null) {
            log.warn("Workspace is null, returning empty artifacts");
            return ArtifactUploadResult.empty();
        }

        try {
            ensureBucketExists();
        } catch (Exception e) {
            log.error("Failed to ensure bucket exists. bucket={}", minioProperties.getBucketResults(), e);
            return ArtifactUploadResult.failure("BUCKET_NOT_AVAILABLE", e.getMessage());
        }

        List<ArtifactItemDto> artifacts = new ArrayList<>();
        long totalSize = 0;

        // 1. 上传日志文件
        artifacts.addAll(uploadLogArtifacts(workspace, taskNo, attemptNo));

        // 2. 上传结果 JSON 文件
        artifacts.addAll(uploadResultJsonArtifact(workspace, taskNo, attemptNo));

        // 3. 上传 output 目录中的所有文件
        artifacts.addAll(uploadOutputArtifacts(workspace, taskNo, attemptNo));

        // 计算总大小
        for (ArtifactItemDto artifact : artifacts) {
            if (artifact.fileSize() != null) {
                totalSize += artifact.fileSize();
            }
        }

        log.info("Artifacts collected and uploaded. taskNo={}, count={}, totalSize={}",
                taskNo, artifacts.size(), totalSize);

        return ArtifactUploadResult.success(artifacts, artifacts.size(), totalSize);
    }

    /**
     * 上传日志产物（stdout.log 和 stderr.log）
     */
    private List<ArtifactItemDto> uploadLogArtifacts(
            WorkspaceManager.WorkspaceContext workspace,
            String taskNo,
            Integer attemptNo) {

        List<ArtifactItemDto> artifacts = new ArrayList<>();

        // 上传 stdout.log
        Path stdoutLog = workspace.stdoutLog();
        if (Files.exists(stdoutLog)) {
            uploadSingleFile(stdoutLog, taskNo, attemptNo, ArtifactType.LOG_STDOUT)
                    .ifPresent(artifacts::add);
        }

        // 上传 stderr.log
        Path stderrLog = workspace.stderrLog();
        if (Files.exists(stderrLog)) {
            uploadSingleFile(stderrLog, taskNo, attemptNo, ArtifactType.LOG_STDERR)
                    .ifPresent(artifacts::add);
        }

        return artifacts;
    }

    /**
     * 上传 result.json 产物
     */
    private List<ArtifactItemDto> uploadResultJsonArtifact(
            WorkspaceManager.WorkspaceContext workspace,
            String taskNo,
            Integer attemptNo) {

        List<ArtifactItemDto> artifacts = new ArrayList<>();
        Path resultJson = workspace.resultJson();

        if (Files.exists(resultJson)) {
            uploadSingleFile(resultJson, taskNo, attemptNo, ArtifactType.RESULT_JSON)
                    .ifPresent(artifacts::add);
        }

        return artifacts;
    }

    /**
     * 上传 output 目录中的所有产物文件
     */
    private List<ArtifactItemDto> uploadOutputArtifacts(
            WorkspaceManager.WorkspaceContext workspace,
            String taskNo,
            Integer attemptNo) {

        List<ArtifactItemDto> artifacts = new ArrayList<>();
        Path outputDir = workspace.outputDir();

        if (!Files.exists(outputDir) || !Files.isDirectory(outputDir)) {
            log.debug("Output directory does not exist or is not a directory. path={}", outputDir);
            return artifacts;
        }

        try (Stream<Path> stream = Files.walk(outputDir, 1)) {
            stream
                    .filter(Files::isRegularFile)
                    .filter(path -> !path.getFileName().toString().equals("result.json"))
                    .forEach(file -> {
                        uploadSingleFile(file, taskNo, attemptNo, ArtifactType.OUTPUT)
                                .ifPresent(artifacts::add);
                    });
        } catch (IOException e) {
            log.warn("Failed to walk output directory. path={}", outputDir, e);
        }

        return artifacts;
    }

    /**
     * 上传单个文件到 MinIO
     */
    private Optional<ArtifactItemDto> uploadSingleFile(
            Path localFile,
            String taskNo,
            Integer attemptNo,
            ArtifactType artifactType) {

        if (!Files.exists(localFile) || !Files.isRegularFile(localFile)) {
            log.debug("File does not exist or is not a regular file. path={}", localFile);
            return Optional.empty();
        }

        String fileName = localFile.getFileName().toString();
        String objectKey = buildObjectKey(taskNo, attemptNo, artifactType.getTypeName(), fileName);
        String mediaType = detectMediaType(fileName);

        try {
            // 计算 SHA-256 校验和
            String checksum = computeSha256(localFile);

            // 获取文件大小
            long fileSize = Files.size(localFile);

            // 上传文件
            // MinIO SDK 要求 multipart upload 的 part size >= 5MiB
            // 对于小文件，使用 fileSize 作为 partSize（单 part 上传）
            // 对于大文件，使用配置的 uploadBufferSize
            long partSize = Math.max(fileSize, MIN_PART_SIZE);
            try (InputStream inputStream = Files.newInputStream(localFile)) {
                PutObjectArgs args = PutObjectArgs.builder()
                        .bucket(minioProperties.getBucketResults())
                        .object(objectKey)
                        .stream(inputStream, fileSize, partSize)
                        .contentType(mediaType)
                        .build();
                minioClient.putObject(args);
            }

            String storageUri = String.format("%s/%s/%s",
                    minioProperties.getEndpoint(),
                    minioProperties.getBucketResults(),
                    objectKey);

            log.debug("File uploaded successfully. fileName={}, objectKey={}, size={}, checksum={}",
                    fileName, objectKey, fileSize, checksum);

            return Optional.of(new ArtifactItemDto(
                    artifactType.getTypeName(),
                    fileName,
                    storageUri,
                    minioProperties.getBucketResults(),
                    objectKey,
                    mediaType,
                    fileSize,
                    checksum
            ));

        } catch (Exception e) {
            log.warn("Failed to upload file. fileName={}, objectKey={}, error={}",
                    fileName, objectKey, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 构建 MinIO ObjectKey
     * 格式: {taskNo}/{attemptNo}/{artifactType}/{fileName}
     */
    private String buildObjectKey(String taskNo, Integer attemptNo, String artifactType, String fileName) {
        String safeTaskNo = sanitizeSegment(taskNo == null ? "unknown" : taskNo);
        String safeAttemptNo = attemptNo == null ? "0" : String.valueOf(attemptNo);
        return String.format("%s/%s/%s/%s", safeTaskNo, safeAttemptNo, artifactType, fileName);
    }

    /**
     * 计算文件的 SHA-256 校验和
     */
    private String computeSha256(Path file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes;

            try (InputStream inputStream = Files.newInputStream(file)) {
                byte[] buffer = new byte[minioProperties.getBufferSize()];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
                hashBytes = digest.digest();
            }

            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException | IOException e) {
            log.warn("Failed to compute SHA-256 for file. path={}, error={}", file, e.getMessage());
            return null;
        }
    }

    /**
     * 根据文件扩展名检测 MIME 类型
     */
    private String detectMediaType(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            String extension = fileName.substring(lastDot).toLowerCase();
            return EXTENSION_TO_MEDIA_TYPE.getOrDefault(extension, DEFAULT_MEDIA_TYPE);
        }
        return DEFAULT_MEDIA_TYPE;
    }

    /**
     * 确保 bucket 存在，如果不存在则创建
     */
    private void ensureBucketExists() throws Exception {
        if (!minioProperties.isAutoCreateBucket()) {
            // 检查 bucket 是否存在
            try {
                minioClient.bucketExists(BucketExistsArgs.builder()
                        .bucket(minioProperties.getBucketResults())
                        .build());
                return;
            } catch (Exception e) {
                log.debug("Bucket does not exist, will try to create. bucket={}", minioProperties.getBucketResults());
            }
        }

        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(minioProperties.getBucketResults())
                .build());

        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(minioProperties.getBucketResults())
                    .build());
            log.info("Created MinIO bucket. bucket={}", minioProperties.getBucketResults());
        }
    }

    /**
     * 清理文件名中的非法字符
     */
    private String sanitizeSegment(String raw) {
        if (raw == null) {
            return "";
        }
        return raw
                .replaceAll("[\\\\/:*?\"<>|\\s]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+", "")
                .replaceAll("_+$", "");
    }
}
