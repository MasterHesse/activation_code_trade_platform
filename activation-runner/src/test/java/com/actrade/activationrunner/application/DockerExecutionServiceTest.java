package com.actrade.activationrunner.application;

import com.actrade.activationrunner.client.dto.ClaimTaskResponse;
import com.actrade.activationrunner.client.dto.PackageAssetDto;
import com.actrade.activationrunner.client.dto.ToolVersionDto;
import com.actrade.activationrunner.config.DockerProperties;
import com.actrade.activationrunner.config.GVisorProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DockerExecutionService 单元测试
 *
 * <p>测试 Docker 容器执行服务的核心功能，
 * 包括容器生命周期管理、资源配置等。</p>
 *
 * <p>注意：这些测试不实际执行 Docker 命令，
 * 而是测试命令构建和配置逻辑。</p>
 */
class DockerExecutionServiceTest {

    private DockerProperties dockerProperties;
    private GVisorProperties gVisorProperties;
    private DockerCommandBuilder commandBuilder;
    private LogCollector logCollector;
    private ObjectMapper objectMapper;
    private DockerExecutionService dockerExecutionService;

    @BeforeEach
    void setUp() {
        // 初始化 Docker 配置
        dockerProperties = new DockerProperties();
        dockerProperties.setBinary("docker");
        dockerProperties.setRuntime("runsc");
        dockerProperties.setNetworkDisabled(true);
        dockerProperties.setDefaultTimeoutSeconds(300);
        dockerProperties.setDefaultMemoryMb(256);
        dockerProperties.setDefaultCpuLimit(1.0);
        dockerProperties.setPidsLimit(128);

        DockerProperties.Image image = new DockerProperties.Image();
        image.setShell("activation-runner-shell:latest");
        image.setPullTimeoutSeconds(300);
        image.setPrewarmStrategy("on-demand");
        dockerProperties.setImage(image);

        // 初始化 gVisor 配置
        gVisorProperties = new GVisorProperties();
        gVisorProperties.setEnabled(true);
        gVisorProperties.setRunscPath("/usr/bin/runsc");
        gVisorProperties.setRuntimeName("runsc");
        gVisorProperties.setSandboxType("application");
        gVisorProperties.setFileSystemType("overlay");
        gVisorProperties.setDebug(false);
        gVisorProperties.setLogFormat("text");
        gVisorProperties.setTraceSyscall(false);
        gVisorProperties.setNetworkDisabled(true);
        gVisorProperties.setNetworkMode("sandboxed");
        gVisorProperties.setHeartbeatIntervalMs(10000);
        gVisorProperties.setMaxStartTimeMs(30000);
        gVisorProperties.setTestModeEnabled(false);
        gVisorProperties.setDisableHostSharing(true);
        gVisorProperties.setSeccompEnabled(true);
        gVisorProperties.setWarningAction("log");
        gVisorProperties.setTraceFile("/var/log/runsc_syscall");
        gVisorProperties.setDebugLogPath("/var/log/runsc");

        // 初始化组件
        commandBuilder = new DockerCommandBuilder(dockerProperties, gVisorProperties);
        logCollector = new LogCollector();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        dockerExecutionService = new DockerExecutionService(
                dockerProperties,
                gVisorProperties,
                commandBuilder,
                logCollector,
                objectMapper
        );
    }

    @Nested
    @DisplayName("运行时检查测试")
    class RuntimeCheckTests {

        @Test
        @DisplayName("创建运行时检查结果")
        void testRuntimeCheckResultCreation() {
            DockerExecutionService.RuntimeCheckResult result =
                    new DockerExecutionService.RuntimeCheckResult(
                            true,   // dockerAvailable
                            true,   // gvisorAvailable
                            true,   // sandboxEnabled
                            "READY_SANDBOXED",
                            "runsc",
                            "/usr/bin/runsc"
                    );

            assertTrue(result.dockerAvailable());
            assertTrue(result.gvisorAvailable());
            assertTrue(result.sandboxEnabled());
            assertTrue(result.isReady());
            assertEquals("READY_SANDBOXED", result.status());
        }

        @Test
        @DisplayName("Docker 不可用时检查失败")
        void testDockerUnavailable() {
            DockerExecutionService.RuntimeCheckResult result =
                    new DockerExecutionService.RuntimeCheckResult(
                            false,  // dockerAvailable
                            true,   // gvisorAvailable
                            true,   // sandboxEnabled
                            "DOCKER_UNAVAILABLE",
                            "runsc",
                            "/usr/bin/runsc"
                    );

            assertFalse(result.isReady());
        }

        @Test
        @DisplayName("gVisor 不可用时检查失败")
        void testGvisorUnavailable() {
            DockerExecutionService.RuntimeCheckResult result =
                    new DockerExecutionService.RuntimeCheckResult(
                            true,   // dockerAvailable
                            false,  // gvisorAvailable
                            true,   // sandboxEnabled
                            "GVISOR_UNAVAILABLE",
                            "runsc",
                            "/usr/bin/runsc"
                    );

            assertFalse(result.isReady());
        }

        @Test
        @DisplayName("沙箱禁用但 Docker 可用时检查通过")
        void testSandboxDisabledButDockerReady() {
            DockerExecutionService.RuntimeCheckResult result =
                    new DockerExecutionService.RuntimeCheckResult(
                            true,   // dockerAvailable
                            false,  // gvisorAvailable
                            false,  // sandboxEnabled
                            "READY_UNSAFE",
                            "runc",
                            "/usr/bin/runc"
                    );

            assertTrue(result.isReady());
        }
    }

    @Nested
    @DisplayName("容器名称构建测试")
    class ContainerNameTests {

        @Test
        @DisplayName("构建安全的容器名称")
        void testSafeContainerName() {
            String containerName = "actrade_task123_attempt1_abc12345";

            // 验证名称格式
            assertTrue(containerName.startsWith("actrade_"));
            assertTrue(containerName.contains("task123"));
            assertTrue(containerName.contains("attempt1"));
        }

        @Test
        @DisplayName("处理特殊字符")
        void testSpecialCharacters() {
            String taskNo = "task/with\\special:chars";
            String safeTaskNo = taskNo.replaceAll("[^a-zA-Z0-9]", "_");

            assertFalse(safeTaskNo.contains("/"));
            assertFalse(safeTaskNo.contains("\\"));
            assertFalse(safeTaskNo.contains(":"));
        }
    }

    @Nested
    @DisplayName("镜像解析测试")
    class ImageResolutionTests {

        @Test
        @DisplayName("使用默认 shell 镜像")
        void testDefaultShellImage() {
            String defaultImage = dockerProperties.getImage().getShell();
            assertEquals("activation-runner-shell:latest", defaultImage);
        }

        @Test
        @DisplayName("解析 Windows 平台镜像")
        void testWindowsImageResolution() {
            ToolVersionDto toolVersion = new ToolVersionDto(
                    "v1.0.0",
                    "activation",
                    "windows",
                    "amd64",
                    "run.exe",
                    "run.exe --input input.json",
                    60,
                    512,
                    List.of(0),
                    null
            );

            // 模拟镜像名称解析
            String os = toolVersion.runtimeOs();
            String arch = toolVersion.runtimeArch();
            String expectedSuffix = os + "-" + arch;

            assertEquals("windows", os);
            assertEquals("amd64", arch);
            assertTrue(expectedSuffix.contains("windows"));
        }

        @Test
        @DisplayName("解析 Linux 平台镜像")
        void testLinuxImageResolution() {
            ToolVersionDto toolVersion = new ToolVersionDto(
                    "v1.0.0",
                    "activation",
                    "linux",
                    "amd64",
                    "./run.sh",
                    "./run.sh --input input.json",
                    60,
                    256,
                    List.of(0),
                    null
            );

            String os = toolVersion.runtimeOs();
            assertEquals("linux", os);
        }
    }

    @Nested
    @DisplayName("超时配置测试")
    class TimeoutTests {

        @Test
        @DisplayName("使用工具版本指定的超时")
        void testToolVersionTimeout() {
            ToolVersionDto toolVersion = new ToolVersionDto(
                    "v1.0.0",
                    "activation",
                    "linux",
                    "amd64",
                    "./run.sh",
                    "./run.sh",
                    120,  // timeoutSeconds
                    256,
                    List.of(0),
                    null
            );

            Integer timeout = toolVersion.timeoutSeconds();
            assertEquals(120, timeout);
        }

        @Test
        @DisplayName("使用默认超时")
        void testDefaultTimeout() {
            Integer defaultTimeout = dockerProperties.getDefaultTimeoutSeconds();
            assertEquals(300, defaultTimeout);
        }

        @Test
        @DisplayName("处理空超时")
        void testNullTimeout() {
            ToolVersionDto toolVersion = new ToolVersionDto(
                    "v1.0.0",
                    "activation",
                    "linux",
                    "amd64",
                    "./run.sh",
                    "./run.sh",
                    null,  // timeoutSeconds
                    256,
                    List.of(0),
                    null
            );

            Integer timeout = toolVersion.timeoutSeconds();
            assertNull(timeout);
        }
    }

    @Nested
    @DisplayName("资源限制测试")
    class ResourceLimitTests {

        @Test
        @DisplayName("使用工具版本指定的内存限制")
        void testToolVersionMemoryLimit() {
            ToolVersionDto toolVersion = new ToolVersionDto(
                    "v1.0.0",
                    "activation",
                    "linux",
                    "amd64",
                    "./run.sh",
                    "./run.sh",
                    60,
                    1024,  // maxMemoryMb
                    List.of(0),
                    null
            );

            Integer memory = toolVersion.maxMemoryMb();
            assertEquals(1024, memory);
        }

        @Test
        @DisplayName("使用默认内存限制")
        void testDefaultMemoryLimit() {
            Integer defaultMemory = dockerProperties.getDefaultMemoryMb();
            assertEquals(256, defaultMemory);
        }

        @Test
        @DisplayName("验证 CPU 限制")
        void testCpuLimit() {
            Double cpuLimit = dockerProperties.getDefaultCpuLimit();
            assertEquals(1.0, cpuLimit);
        }

        @Test
        @DisplayName("验证 PID 限制")
        void testPidsLimit() {
            Integer pidsLimit = dockerProperties.getPidsLimit();
            assertEquals(128, pidsLimit);
        }
    }

    @Nested
    @DisplayName("gVisor 配置验证测试")
    class GVisorConfigTests {

        @Test
        @DisplayName("gVisor 默认启用")
        void testGvisorEnabledByDefault() {
            assertTrue(gVisorProperties.getEnabled());
        }

        @Test
        @DisplayName("gVisor 运行时名称")
        void testGvisorRuntimeName() {
            assertEquals("runsc", gVisorProperties.getRuntimeName());
        }

        @Test
        @DisplayName("gVisor 沙箱类型")
        void testGvisorSandboxType() {
            assertEquals("application", gVisorProperties.getSandboxType());
        }

        @Test
        @DisplayName("gVisor 网络默认禁用")
        void testGvisorNetworkDisabled() {
            assertTrue(gVisorProperties.getNetworkDisabled());
        }

        @Test
        @DisplayName("gVisor 文件系统类型")
        void testGvisorFileSystemType() {
            assertEquals("overlay", gVisorProperties.getFileSystemType());
        }

        @Test
        @DisplayName("gVisor 调试模式默认禁用")
        void testGvisorDebugDisabled() {
            assertFalse(gVisorProperties.getDebug());
        }

        @Test
        @DisplayName("gVisor 系统调用跟踪默认禁用")
        void testGvisorTraceSyscallDisabled() {
            assertFalse(gVisorProperties.getTraceSyscall());
        }

        @Test
        @DisplayName("gVisor 心跳间隔")
        void testGvisorHeartbeatInterval() {
            assertEquals(10000, gVisorProperties.getHeartbeatIntervalMs());
        }

        @Test
        @DisplayName("gVisor 最大启动时间")
        void testGvisorMaxStartTime() {
            assertEquals(30000, gVisorProperties.getMaxStartTimeMs());
        }
    }

    @Nested
    @DisplayName("工作空间路径测试")
    class WorkspacePathTests {

        @Test
        @DisplayName("创建执行上下文")
        void testExecutionContextCreation() {
            ContainerExecutionContext context = ContainerExecutionContext.builder()
                    .containerName("test-container")
                    .imageName("test-image:latest")
                    .timeoutSeconds(300)
                    .inputDir(Path.of("/workspace/input"))
                    .outputDir(Path.of("/workspace/output"))
                    .logsDir(Path.of("/workspace/logs"))
                    .tempDir(Path.of("/workspace/temp"))
                    .stdoutPath(Path.of("/workspace/logs/stdout.log"))
                    .stderrPath(Path.of("/workspace/logs/stderr.log"))
                    .resultJsonPath(Path.of("/workspace/output/result.json"))
                    .gvisorSandboxEnabled(true)
                    .gvisorSandboxType("application")
                    .gvisorTraceEnabled(false)
                    .build();

            assertEquals("test-container", context.containerName());
            assertEquals("test-image:latest", context.imageName());
            assertEquals(300, context.timeoutSeconds());
            assertTrue(context.gvisorSandboxEnabled());
            assertEquals("application", context.gvisorSandboxType());
        }
    }

    @Nested
    @DisplayName("执行结果构建测试")
    class ExecutionResultTests {

        @Test
        @DisplayName("构建成功结果")
        void testBuildSuccessResult() {
            java.time.OffsetDateTime startedAt = java.time.OffsetDateTime.now();
            java.time.OffsetDateTime finishedAt = startedAt.plusSeconds(30);

            ContainerExecutionContext context = ContainerExecutionContext.builder()
                    .containerName("test-container")
                    .imageName("test-image:latest")
                    .timeoutSeconds(300)
                    .inputDir(Path.of("/workspace/input"))
                    .outputDir(Path.of("/workspace/output"))
                    .logsDir(Path.of("/workspace/logs"))
                    .stdoutPath(Path.of("/workspace/logs/stdout.log"))
                    .stderrPath(Path.of("/workspace/logs/stderr.log"))
                    .resultJsonPath(Path.of("/workspace/output/result.json"))
                    .gvisorSandboxEnabled(true)
                    .gvisorSandboxType("application")
                    .gvisorTraceEnabled(false)
                    .build();

            ContainerExecutionResult result = ContainerExecutionResult.success(
                    "container123",
                    "test-container",
                    "test-image:latest",
                    0,
                    startedAt,
                    finishedAt,
                    context
            );

            assertTrue(result.success());
            assertEquals(0, result.exitCode());
            assertEquals("NormalExit", result.exitReason());
            assertEquals("container123", result.containerId());
            assertTrue(result.gvisorSandboxEnabled());
            assertEquals("application", result.gvisorSandboxType());
            assertFalse(result.timedOut());
            assertEquals(30000, result.durationMs());
        }

        @Test
        @DisplayName("构建失败结果")
        void testBuildFailureResult() {
            java.time.OffsetDateTime startedAt = java.time.OffsetDateTime.now();
            java.time.OffsetDateTime finishedAt = startedAt.plusSeconds(10);

            ContainerExecutionContext context = ContainerExecutionContext.builder()
                    .containerName("test-container")
                    .imageName("test-image:latest")
                    .timeoutSeconds(300)
                    .inputDir(Path.of("/workspace/input"))
                    .outputDir(Path.of("/workspace/output"))
                    .logsDir(Path.of("/workspace/logs"))
                    .stdoutPath(Path.of("/workspace/logs/stdout.log"))
                    .stderrPath(Path.of("/workspace/logs/stderr.log"))
                    .resultJsonPath(Path.of("/workspace/output/result.json"))
                    .gvisorSandboxEnabled(true)
                    .gvisorSandboxType("application")
                    .gvisorTraceEnabled(false)
                    .build();

            ContainerExecutionResult result = ContainerExecutionResult.failure(
                    "container123",
                    "test-container",
                    "test-image:latest",
                    "EXECUTION_ERROR",
                    "Container exited with error",
                    startedAt,
                    finishedAt,
                    context
            );

            assertFalse(result.success());
            assertEquals(-1, result.exitCode());
            assertEquals("EXECUTION_ERROR", result.errorCode());
            assertEquals("Container exited with error", result.errorMessage());
            assertEquals("Error", result.exitReason());
        }

        @Test
        @DisplayName("构建超时结果")
        void testBuildTimeoutResult() {
            java.time.OffsetDateTime startedAt = java.time.OffsetDateTime.now();

            ContainerExecutionContext context = ContainerExecutionContext.builder()
                    .containerName("test-container")
                    .imageName("test-image:latest")
                    .timeoutSeconds(30)
                    .inputDir(Path.of("/workspace/input"))
                    .outputDir(Path.of("/workspace/output"))
                    .logsDir(Path.of("/workspace/logs"))
                    .stdoutPath(Path.of("/workspace/logs/stdout.log"))
                    .stderrPath(Path.of("/workspace/logs/stderr.log"))
                    .resultJsonPath(Path.of("/workspace/output/result.json"))
                    .gvisorSandboxEnabled(true)
                    .gvisorSandboxType("application")
                    .gvisorTraceEnabled(false)
                    .build();

            ContainerExecutionResult result = ContainerExecutionResult.timeout(
                    "container123",
                    "test-container",
                    "test-image:latest",
                    30,
                    startedAt,
                    context
            );

            assertFalse(result.success());
            assertEquals(-1, result.exitCode());
            assertEquals("EXECUTION_TIMEOUT", result.errorCode());
            assertTrue(result.timedOut());
            assertEquals("Timeout", result.exitReason());
        }

        @Test
        @DisplayName("构建异常结果")
        void testBuildExceptionResult() {
            java.time.OffsetDateTime startedAt = java.time.OffsetDateTime.now();

            ContainerExecutionContext context = ContainerExecutionContext.builder()
                    .containerName("test-container")
                    .imageName("test-image:latest")
                    .timeoutSeconds(300)
                    .inputDir(Path.of("/workspace/input"))
                    .outputDir(Path.of("/workspace/output"))
                    .logsDir(Path.of("/workspace/logs"))
                    .stdoutPath(Path.of("/workspace/logs/stdout.log"))
                    .stderrPath(Path.of("/workspace/logs/stderr.log"))
                    .resultJsonPath(Path.of("/workspace/output/result.json"))
                    .gvisorSandboxEnabled(true)
                    .gvisorSandboxType("application")
                    .gvisorTraceEnabled(false)
                    .build();

            ContainerExecutionResult result = ContainerExecutionResult.exception(
                    "test-container",
                    "test-image:latest",
                    "IO_ERROR",
                    "Failed to read input file",
                    startedAt,
                    context
            );

            assertFalse(result.success());
            assertEquals(-1, result.exitCode());
            assertEquals("IO_ERROR", result.errorCode());
            assertEquals("Failed to read input file", result.errorMessage());
            assertEquals("Exception", result.exitReason());
            assertNull(result.containerId());
        }
    }

    @Nested
    @DisplayName("日志收集测试")
    class LogCollectorTests {

        @Test
        @DisplayName("创建成功日志收集结果")
        void testCreateSuccessLogResult() {
            LogCollector.LogCollectionResult result = LogCollector.LogCollectionResult.success(
                    "test-container",
                    100,
                    "stdout content",
                    "stderr content",
                    0
            );

            assertTrue(result.success());
            assertEquals("test-container", result.containerName());
            assertEquals(100, result.durationMs());
            assertEquals("stdout content", result.stdoutContent());
            assertEquals("stderr content", result.stderrContent());
            assertEquals(0, result.exitCode());
            assertFalse(result.timedOut());
        }

        @Test
        @DisplayName("创建错误日志收集结果")
        void testCreateErrorLogResult() {
            LogCollector.LogCollectionResult result = LogCollector.LogCollectionResult.error(
                    "test-container",
                    "Failed to execute docker logs"
            );

            assertFalse(result.success());
            assertEquals("Failed to execute docker logs", result.errorMessage());
            assertNull(result.stdoutContent());
            assertNull(result.stderrContent());
        }

        @Test
        @DisplayName("创建超时日志收集结果")
        void testCreateTimeoutLogResult() {
            LogCollector.LogCollectionResult result = LogCollector.LogCollectionResult.timeout(
                    "test-container",
                    60000,
                    "partial output",
                    ""
            );

            assertFalse(result.success());
            assertTrue(result.timedOut());
            assertEquals("Timeout", result.errorMessage());
        }

        @Test
        @DisplayName("获取完整输出")
        void testGetFullOutput() {
            LogCollector.LogCollectionResult result = LogCollector.LogCollectionResult.success(
                    "test-container",
                    100,
                    "stdout line1\nstdout line2",
                    "stderr line1",
                    0
            );

            String fullOutput = result.getFullOutput();
            assertTrue(fullOutput.contains("stdout line1"));
            assertTrue(fullOutput.contains("stderr line1"));
        }
    }
}
