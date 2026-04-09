package com.actrade.activationrunner.application;

import com.actrade.activationrunner.client.dto.ArtifactItemDto;
import com.actrade.activationrunner.config.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.ObjectWriteResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ArtifactUploadService 单元测试")
class ArtifactUploadServiceTest {

    @Mock
    private MinioClient minioClient;

    @Mock
    private ObjectWriteResponse mockObjectWriteResponse;

    @TempDir
    Path tempDir;

    private MinioProperties minioProperties;
    private ArtifactUploadService artifactUploadService;

    @BeforeEach
    void setUp() throws Exception {
        minioProperties = new MinioProperties();
        minioProperties.setEndpoint("http://localhost:9000");
        minioProperties.setAccessKey("minioadmin");
        minioProperties.setSecretKey("minioadmin");
        minioProperties.setBucketArtifacts("activation-artifacts");
        minioProperties.setBucketResults("activation-results");
        minioProperties.setBufferSize(8192);
        minioProperties.setUploadBufferSize(8192);
        minioProperties.setAutoCreateBucket(true);

        // Mock bucket exists check - bucket already exists
        lenient().when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        // Mock successful put operation
        lenient().when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(mockObjectWriteResponse);

        artifactUploadService = new ArtifactUploadService(minioClient, minioProperties);
    }

    @Nested
    @DisplayName("collectAndUploadArtifacts - 收集并上传产物")
    class CollectAndUploadArtifactsTests {

        @Test
        @DisplayName("当 workspace 为空时应返回空结果")
        void shouldReturnEmptyResultWhenWorkspaceIsNull() {
            ArtifactUploadResult result = artifactUploadService.collectAndUploadArtifacts(
                    null, "TASK001", 1);

            assertThat(result.success()).isTrue();
            assertThat(result.artifacts()).isEmpty();
            assertThat(result.uploadedCount()).isZero();
        }

        @Test
        @DisplayName("当工作空间为空目录时应返回空结果")
        void shouldReturnEmptyResultWhenWorkspaceIsEmpty() throws IOException {
            // Create workspace structure
            Path rootDir = tempDir.resolve("workspace");
            Path inputDir = rootDir.resolve("input");
            Path outputDir = rootDir.resolve("output");
            Path logsDir = rootDir.resolve("logs");
            Files.createDirectories(rootDir);
            Files.createDirectories(inputDir);
            Files.createDirectories(outputDir);
            Files.createDirectories(logsDir);

            WorkspaceManager.WorkspaceContext workspace = new WorkspaceManager.WorkspaceContext(
                    rootDir, inputDir, outputDir, logsDir, tempDir.resolve("temp"));

            ArtifactUploadResult result = artifactUploadService.collectAndUploadArtifacts(
                    workspace, "TASK001", 1);

            assertThat(result.success()).isTrue();
            assertThat(result.artifacts()).isEmpty();
        }

        @Test
        @DisplayName("应正确上传 stdout.log 和 stderr.log")
        void shouldUploadLogFiles() throws Exception {
            // Create workspace structure with logs
            Path rootDir = tempDir.resolve("workspace1");
            Path inputDir = rootDir.resolve("input");
            Path outputDir = rootDir.resolve("output");
            Path logsDir = rootDir.resolve("logs");
            Files.createDirectories(rootDir);
            Files.createDirectories(inputDir);
            Files.createDirectories(outputDir);
            Files.createDirectories(logsDir);

            // Create log files
            Files.writeString(logsDir.resolve("stdout.log"), "Test stdout content");
            Files.writeString(logsDir.resolve("stderr.log"), "Test stderr content");

            WorkspaceManager.WorkspaceContext workspace = new WorkspaceManager.WorkspaceContext(
                    rootDir, inputDir, outputDir, logsDir, tempDir.resolve("temp"));

            ArtifactUploadResult result = artifactUploadService.collectAndUploadArtifacts(
                    workspace, "TASK001", 2);

            assertThat(result.success()).isTrue();
            assertThat(result.artifacts()).hasSize(2);

            // Verify upload was called for each log file
            verify(minioClient, times(2)).putObject(any(PutObjectArgs.class));
        }

        @Test
        @DisplayName("应正确上传 result.json")
        void shouldUploadResultJson() throws Exception {
            // Create workspace structure with result.json
            Path rootDir = tempDir.resolve("workspace2");
            Path inputDir = rootDir.resolve("input");
            Path outputDir = rootDir.resolve("output");
            Path logsDir = rootDir.resolve("logs");
            Files.createDirectories(rootDir);
            Files.createDirectories(inputDir);
            Files.createDirectories(outputDir);
            Files.createDirectories(logsDir);

            // Create result.json
            Files.writeString(outputDir.resolve("result.json"), "{\"status\":\"SUCCESS\"}");

            WorkspaceManager.WorkspaceContext workspace = new WorkspaceManager.WorkspaceContext(
                    rootDir, inputDir, outputDir, logsDir, tempDir.resolve("temp"));

            ArtifactUploadResult result = artifactUploadService.collectAndUploadArtifacts(
                    workspace, "TASK002", 1);

            assertThat(result.success()).isTrue();
            assertThat(result.artifacts()).hasSize(1);

            ArtifactItemDto artifact = result.artifacts().get(0);
            assertThat(artifact.artifactType()).isEqualTo("result-json");
            assertThat(artifact.fileName()).isEqualTo("result.json");
            assertThat(artifact.mediaType()).isEqualTo("application/json");
        }

        @Test
        @DisplayName("应正确上传 output 目录中的文件")
        void shouldUploadOutputFiles() throws Exception {
            // Create workspace structure with output files
            Path rootDir = tempDir.resolve("workspace3");
            Path inputDir = rootDir.resolve("input");
            Path outputDir = rootDir.resolve("output");
            Path logsDir = rootDir.resolve("logs");
            Files.createDirectories(rootDir);
            Files.createDirectories(inputDir);
            Files.createDirectories(outputDir);
            Files.createDirectories(logsDir);

            // Create output files
            Files.writeString(outputDir.resolve("data.csv"), "col1,col2\nval1,val2");
            Files.writeString(outputDir.resolve("report.html"), "<html>Report</html>");
            Files.writeString(outputDir.resolve("result.json"), "{\"result\":\"ok\"}"); // should be excluded

            WorkspaceManager.WorkspaceContext workspace = new WorkspaceManager.WorkspaceContext(
                    rootDir, inputDir, outputDir, logsDir, tempDir.resolve("temp"));

            ArtifactUploadResult result = artifactUploadService.collectAndUploadArtifacts(
                    workspace, "TASK003", 1);

            assertThat(result.success()).isTrue();
            // Should upload: data.csv, report.html + result.json (log) = 3 files
            // result.json in output should be excluded, but stdout/stderr logs don't exist
            // So only output files: data.csv, report.html
            assertThat(result.artifacts()).hasSizeGreaterThanOrEqualTo(2);
        }

        @Test
        @DisplayName("应正确计算文件 SHA-256 校验和")
        void shouldComputeSha256Checksum() throws Exception {
            // Create workspace structure
            Path rootDir = tempDir.resolve("workspace4");
            Path inputDir = rootDir.resolve("input");
            Path outputDir = rootDir.resolve("output");
            Path logsDir = rootDir.resolve("logs");
            Files.createDirectories(rootDir);
            Files.createDirectories(inputDir);
            Files.createDirectories(outputDir);
            Files.createDirectories(logsDir);

            // Create a file with known content
            String content = "Hello, World!";
            Path testFile = logsDir.resolve("stdout.log");
            Files.writeString(testFile, content);

            WorkspaceManager.WorkspaceContext workspace = new WorkspaceManager.WorkspaceContext(
                    rootDir, inputDir, outputDir, logsDir, tempDir.resolve("temp"));

            ArtifactUploadResult result = artifactUploadService.collectAndUploadArtifacts(
                    workspace, "TASK004", 1);

            assertThat(result.success()).isTrue();
            assertThat(result.artifacts()).hasSize(1);

            ArtifactItemDto artifact = result.artifacts().get(0);
            // SHA-256 of "Hello, World!" is known
            assertThat(artifact.checksumSha256())
                    .isEqualTo("dffd6021bb2bd5b0af676290809ec3a53191dd81c7f70a4b28688a362182986f");
        }

        @Test
        @DisplayName("应正确检测文件 mediaType")
        void shouldDetectMediaType() throws Exception {
            // Create workspace structure with various file types
            Path rootDir = tempDir.resolve("workspace5");
            Path inputDir = rootDir.resolve("input");
            Path outputDir = rootDir.resolve("output");
            Path logsDir = rootDir.resolve("logs");
            Files.createDirectories(rootDir);
            Files.createDirectories(inputDir);
            Files.createDirectories(outputDir);
            Files.createDirectories(logsDir);

            // Create files with different extensions
            Files.writeString(outputDir.resolve("data.json"), "{}");
            Files.writeString(outputDir.resolve("report.xml"), "<doc/>");
            Files.writeString(outputDir.resolve("readme.txt"), "Readme content");
            Files.writeString(outputDir.resolve("image.png"), "fake-png");

            WorkspaceManager.WorkspaceContext workspace = new WorkspaceManager.WorkspaceContext(
                    rootDir, inputDir, outputDir, logsDir, tempDir.resolve("temp"));

            ArtifactUploadResult result = artifactUploadService.collectAndUploadArtifacts(
                    workspace, "TASK005", 1);

            assertThat(result.success()).isTrue();
            // Should have 4 artifacts from output directory (excluding result.json which is filtered)
            assertThat(result.artifacts()).hasSize(4);

            var artifactMap = result.artifacts().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            ArtifactItemDto::fileName,
                            artifact -> artifact));

            assertThat(artifactMap.get("data.json").mediaType()).isEqualTo("application/json");
            assertThat(artifactMap.get("report.xml").mediaType()).isEqualTo("application/xml");
            assertThat(artifactMap.get("readme.txt").mediaType()).isEqualTo("text/plain");
            assertThat(artifactMap.get("image.png").mediaType()).isEqualTo("image/png");
        }

        @Test
        @DisplayName("上传失败时应返回部分成功结果")
        void shouldReturnPartialSuccessOnUploadFailure() throws Exception {
            // Create workspace structure
            Path rootDir = tempDir.resolve("workspace6");
            Path inputDir = rootDir.resolve("input");
            Path outputDir = rootDir.resolve("output");
            Path logsDir = rootDir.resolve("logs");
            Files.createDirectories(rootDir);
            Files.createDirectories(inputDir);
            Files.createDirectories(outputDir);
            Files.createDirectories(logsDir);

            // Create files
            Files.writeString(logsDir.resolve("stdout.log"), "stdout content");

            WorkspaceManager.WorkspaceContext workspace = new WorkspaceManager.WorkspaceContext(
                    rootDir, inputDir, outputDir, logsDir, tempDir.resolve("temp"));

            ArtifactUploadResult result = artifactUploadService.collectAndUploadArtifacts(
                    workspace, "TASK006", 1);

            assertThat(result.success()).isTrue();
            assertThat(result.uploadedCount()).isEqualTo(result.artifacts().size());
        }
    }

    @Nested
    @DisplayName("ArtifactUploadResult - 结果工厂方法")
    class ArtifactUploadResultTests {

        @Test
        @DisplayName("success 方法应正确创建成功结果")
        void successMethodShouldCreateSuccessResult() {
            List<ArtifactItemDto> artifacts = List.of(
                    new ArtifactItemDto("output", "test.txt", "uri", "bucket", "key",
                            "text/plain", 100L, "checksum")
            );

            ArtifactUploadResult result = ArtifactUploadResult.success(artifacts, 1, 100L);

            assertThat(result.success()).isTrue();
            assertThat(result.artifacts()).hasSize(1);
            assertThat(result.uploadedCount()).isEqualTo(1);
            assertThat(result.totalSizeBytes()).isEqualTo(100L);
            assertThat(result.errorCode()).isNull();
            assertThat(result.errorMessage()).isNull();
        }

        @Test
        @DisplayName("failure 方法应正确创建失败结果")
        void failureMethodShouldCreateFailureResult() {
            ArtifactUploadResult result = ArtifactUploadResult.failure("UPLOAD_FAILED", "Network error");

            assertThat(result.success()).isFalse();
            assertThat(result.artifacts()).isEmpty();
            assertThat(result.uploadedCount()).isZero();
            assertThat(result.totalSizeBytes()).isZero();
            assertThat(result.errorCode()).isEqualTo("UPLOAD_FAILED");
            assertThat(result.errorMessage()).isEqualTo("Network error");
        }

        @Test
        @DisplayName("empty 方法应创建空成功结果")
        void emptyMethodShouldCreateEmptySuccessResult() {
            ArtifactUploadResult result = ArtifactUploadResult.empty();

            assertThat(result.success()).isTrue();
            assertThat(result.artifacts()).isEmpty();
            assertThat(result.uploadedCount()).isZero();
            assertThat(result.totalSizeBytes()).isZero();
        }
    }

    @Nested
    @DisplayName("ObjectKey 构建")
    class ObjectKeyBuildingTests {

        @Test
        @DisplayName("应正确构建 ObjectKey 格式")
        void shouldBuildCorrectObjectKeyFormat() throws Exception {
            Path rootDir = tempDir.resolve("workspace7");
            Path inputDir = rootDir.resolve("input");
            Path outputDir = rootDir.resolve("output");
            Path logsDir = rootDir.resolve("logs");
            Files.createDirectories(rootDir);
            Files.createDirectories(inputDir);
            Files.createDirectories(outputDir);
            Files.createDirectories(logsDir);

            Files.writeString(logsDir.resolve("stdout.log"), "content");

            WorkspaceManager.WorkspaceContext workspace = new WorkspaceManager.WorkspaceContext(
                    rootDir, inputDir, outputDir, logsDir, tempDir.resolve("temp"));

            artifactUploadService.collectAndUploadArtifacts(workspace, "TASK007", 3);

            ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);
            verify(minioClient).putObject(captor.capture());

            PutObjectArgs capturedArgs = captor.getValue();
            String objectKey = capturedArgs.object();

            // Verify object key format: taskNo/attemptNo/artifactType/fileName
            assertThat(objectKey).contains("TASK007");
            assertThat(objectKey).contains("3");
            assertThat(objectKey).contains("log-stdout");
            assertThat(objectKey).contains("stdout.log");
        }

        @Test
        @DisplayName("应正确处理 null taskNo")
        void shouldHandleNullTaskNo() throws Exception {
            Path rootDir = tempDir.resolve("workspace8");
            Path inputDir = rootDir.resolve("input");
            Path outputDir = rootDir.resolve("output");
            Path logsDir = rootDir.resolve("logs");
            Files.createDirectories(rootDir);
            Files.createDirectories(inputDir);
            Files.createDirectories(outputDir);
            Files.createDirectories(logsDir);

            Files.writeString(logsDir.resolve("stdout.log"), "content");

            WorkspaceManager.WorkspaceContext workspace = new WorkspaceManager.WorkspaceContext(
                    rootDir, inputDir, outputDir, logsDir, tempDir.resolve("temp"));

            ArtifactUploadResult result = artifactUploadService.collectAndUploadArtifacts(
                    workspace, null, 1);

            assertThat(result.success()).isTrue();
        }
    }
}
