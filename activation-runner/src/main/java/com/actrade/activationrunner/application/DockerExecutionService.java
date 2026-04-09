package com.actrade.activationrunner.application;

import com.actrade.activationrunner.client.dto.ClaimTaskResponse;
import com.actrade.activationrunner.client.dto.ToolVersionDto;
import com.actrade.activationrunner.config.DockerProperties;
import com.actrade.activationrunner.config.GVisorProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Docker 容器执行服务
 *
 * <p>核心服务，负责在 Docker 容器中执行激活任务。</p>
 *
 * <p>主要职责：
 * <ul>
 *   <li>容器生命周期管理 (创建、启动、停止、删除)</li>
 *   <li>镜像拉取和缓存检查</li>
 *   <li>容器资源限制配置</li>
 *   <li>执行日志采集</li>
 *   <li>gVisor 沙箱运行时集成</li>
 *   <li>超时控制和错误处理</li>
 *   <li>性能指标采集</li>
 * </ul>
 * </p>
 *
 * <p>gVisor 沙箱集成说明：
 * <ul>
 *   <li>通过 Docker --runtime 参数使用 runsc 运行时</li>
 *   <li>所有容器默认在 gVisor 沙箱中运行</li>
 *   <li>支持配置文件系统访问模式、网络隔离等</li>
 *   <li>可选启用系统调用跟踪用于调试</li>
 * </ul>
 * </p>
 *
 * @see GVisorProperties
 * @see DockerCommandBuilder
 * @see LogCollector
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DockerExecutionService {

    private final DockerProperties dockerProperties;
    private final GVisorProperties gVisorProperties;
    private final DockerCommandBuilder commandBuilder;
    private final LogCollector logCollector;
    private final ObjectMapper objectMapper;

    /**
     * 执行容器任务
     *
     * <p>完整的容器执行流程：
     * <ol>
     *   <li>创建容器执行上下文</li>
     *   <li>检查/拉取镜像</li>
     *   <li>创建容器</li>
     *   <li>启动容器</li>
     *   <li>等待执行完成</li>
     *   <li>采集日志</li>
     *   <li>清理容器</li>
     *   <li>返回执行结果</li>
     * </ol>
     * </p>
     *
     * @param taskNo 任务编号
     * @param attemptNo 尝试编号
     * @param claimResponse 任务声明响应
     * @param workspace 工作空间上下文
     * @return 容器执行结果
     */
    public ContainerExecutionResult executeContainerTask(
            String taskNo,
            Integer attemptNo,
            ClaimTaskResponse claimResponse,
            WorkspaceManager.WorkspaceContext workspace
    ) {
        OffsetDateTime startedAt = OffsetDateTime.now();
        String containerName = buildContainerName(taskNo, attemptNo);
        String imageName = resolveImageName(claimResponse);
        int timeoutSeconds = resolveTimeout(claimResponse);

        log.info("Starting container execution. taskNo={}, containerName={}, image={}, timeout={}s",
                taskNo, containerName, imageName, timeoutSeconds);

        // 创建执行上下文
        ContainerExecutionContext context = buildExecutionContext(
                containerName, imageName, timeoutSeconds, workspace
        );

        Process process = null;
        String containerId = null;

        try {
            // 步骤 1: 检查镜像是否存在，不存在则拉取
            if (!checkImageExists(imageName)) {
                log.info("Image not found, pulling. image={}", imageName);
                boolean pullSuccess = pullImage(imageName, timeoutSeconds);
                if (!pullSuccess) {
                    return ContainerExecutionResult.exception(
                            containerName, imageName, "IMAGE_PULL_FAILED",
                            "Failed to pull image: " + imageName, startedAt, context
                    );
                }
            }

            // 步骤 2: 清理可能存在的旧容器
            cleanupContainer(containerName);

            // 步骤 3: 创建容器
            ToolVersionDto toolVersion = claimResponse.toolVersion();
            List<String> createCommand = commandBuilder.buildExecutionCommand(
                    containerName,
                    imageName,
                    workspace.inputDir(),
                    toolVersion,
                    parseEnvVars(claimResponse.payloadJson())
            );

            String commandStr = commandBuilder.toCommandString(createCommand);
            log.debug("Creating container with command: {}", commandStr);

            process = executeCommand(createCommand, timeoutSeconds + 30);
            String createOutput = readProcessOutput(process);
            int createExitCode = process.exitValue();

            if (createExitCode != 0) {
                log.error("Container creation failed. container={}, exitCode={}, output={}",
                        containerName, createExitCode, createOutput);
                return ContainerExecutionResult.exception(
                        containerName, imageName, "CONTAINER_CREATE_FAILED",
                        "Container creation failed: " + createOutput, startedAt, context
                );
            }

            // 从输出中提取容器 ID
            containerId = extractContainerId(createOutput);
            log.info("Container created successfully. container={}, containerId={}", containerName, containerId);

            // 步骤 4: 启动容器
            List<String> startCommand = commandBuilder.buildStartCommand(containerName, true);
            process = executeCommand(startCommand, timeoutSeconds);
            String startOutput = readProcessOutput(process);
            int startExitCode = process.exitValue();

            log.info("Container started. container={}, exitCode={}", containerName, startExitCode);

            // 步骤 5: 采集日志 (容器可能已经结束)
            LogCollector.LogCollectionResult logResult = logCollector.collectSync(
                    containerName,
                    context.stdoutPath(),
                    context.stderrPath(),
                    30  // 日志采集超时
            );

            if (!logResult.success()) {
                log.warn("Log collection had issues. container={}, error={}",
                        containerName, logResult.errorMessage());
            }

            // 步骤 6: 获取容器最终状态
            String finalState = getContainerState(containerName);

            // 判断是否成功
            boolean success = startExitCode == 0;
            String exitReason = success ? "NormalExit" : "NonZeroExit";

            // 检查是否超时
            long elapsed = java.time.Duration.between(startedAt, OffsetDateTime.now()).toMillis();
            boolean timedOut = elapsed >= (timeoutSeconds * 1000L);

            // 采集性能指标
            Map<String, String> stats = collectContainerStats(containerId);

            OffsetDateTime finishedAt = OffsetDateTime.now();

            if (timedOut && !success) {
                // 停止并清理超时容器
                stopContainer(containerName, 10);
                return ContainerExecutionResult.timeout(containerId, containerName, imageName,
                        timeoutSeconds, startedAt, context);
            }

            return ContainerExecutionResult.builder()
                    .success(success)
                    .exitCode(startExitCode)
                    .containerId(containerId)
                    .containerName(containerName)
                    .imageName(imageName)
                    .startedAt(startedAt)
                    .finishedAt(finishedAt)
                    .durationMs(java.time.Duration.between(startedAt, finishedAt).toMillis())
                    .timedOut(timedOut)
                    .timeoutSeconds(timeoutSeconds)
                    .stdoutPath(context.stdoutPath())
                    .stderrPath(context.stderrPath())
                    .resultJsonPath(context.resultJsonPath())
                    .inputDir(context.inputDir())
                    .outputDir(context.outputDir())
                    .logsDir(context.logsDir())
                    .exitReason(exitReason)
                    .containerState(finalState)
                    .memoryUsageMbPeak(stats.containsKey("memory") ? Long.parseLong(stats.get("memory")) : null)
                    .cpuUsagePercent(stats.containsKey("cpu") ? Double.parseDouble(stats.get("cpu")) : null)
                    .gvisorSandboxEnabled(gVisorProperties.getEnabled())
                    .gvisorSandboxType(gVisorProperties.getSandboxType())
                    .gvisorTraceEnabled(gVisorProperties.getTraceSyscall())
                    .diagnostics(stats)
                    .executionCommand(commandStr)
                    .build();

        } catch (Exception e) {
            log.error("Container execution failed with exception. container={}", containerName, e);
            return ContainerExecutionResult.exception(
                    containerName,
                    imageName,
                    "EXECUTION_ERROR",
                    e.getMessage(),
                    startedAt,
                    context
            );
        } finally {
            // 步骤 7: 清理容器
            try {
                cleanupContainer(containerName);
            } catch (Exception e) {
                log.warn("Failed to cleanup container. container={}", containerName, e);
            }
        }
    }

    /**
     * 检查镜像是否存在
     */
    public boolean checkImageExists(String imageName) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    dockerProperties.getBinary(),
                    "image",
                    "inspect",
                    imageName
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            boolean completed = process.waitFor(30, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                return false;
            }

            return process.exitValue() == 0;
        } catch (Exception e) {
            log.warn("Failed to check image existence. image={}", imageName, e);
            return false;
        }
    }

    /**
     * 拉取镜像
     *
     * @param imageName 镜像名称
     * @param timeoutSeconds 超时时间
     * @return 是否成功
     */
    public boolean pullImage(String imageName, int timeoutSeconds) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    dockerProperties.getBinary(),
                    "pull",
                    imageName
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // 读取输出以便日志记录
            StringBuilder output = new StringBuilder();
            try (var reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("docker pull: {}", line);
                    output.append(line).append(System.lineSeparator());
                }
            }

            boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                log.error("Image pull timed out. image={}", imageName);
                return false;
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("Image pull failed. image={}, exitCode={}, output={}",
                        imageName, exitCode, output);
                return false;
            }

            log.info("Image pulled successfully. image={}", imageName);
            return true;

        } catch (Exception e) {
            log.error("Failed to pull image. image={}", imageName, e);
            return false;
        }
    }

    /**
     * 创建容器
     *
     * @param containerName 容器名称
     * @param imageName 镜像名称
     * @param workDir 工作目录
     * @param command 启动命令
     * @param envVars 环境变量
     * @param memoryMb 内存限制
     * @param cpuLimit CPU 限制
     * @param networkDisabled 是否禁用网络
     * @return 容器 ID
     */
    public String createContainer(
            String containerName,
            String imageName,
            Path workDir,
            String[] command,
            Map<String, String> envVars,
            Integer memoryMb,
            Double cpuLimit,
            boolean networkDisabled
    ) throws IOException {
        List<String> createCommand = commandBuilder.buildCreateCommand(
                containerName,
                imageName,
                workDir,
                command,
                null,
                null,
                envVars,
                memoryMb,
                cpuLimit,
                null,
                networkDisabled,
                Map.of("actrade.managed", "true")
        );

        Process process = executeCommand(createCommand, 60);
        String output = readProcessOutput(process);
        int exitCode = process.exitValue();

        if (exitCode != 0) {
            throw new IOException("Container creation failed: " + output);
        }

        return extractContainerId(output);
    }

    /**
     * 启动容器
     *
     * @param containerName 容器名称
     * @param timeoutSeconds 超时时间
     * @return 退出码
     */
    public int startContainer(String containerName, int timeoutSeconds) throws IOException {
        List<String> startCommand = commandBuilder.buildStartCommand(containerName, true);
        Process process = executeCommand(startCommand, timeoutSeconds);
        return process.exitValue();
    }

    /**
     * 停止容器
     *
     * @param containerName 容器名称
     * @param timeoutSeconds 停止超时
     */
    public void stopContainer(String containerName, int timeoutSeconds) {
        try {
            List<String> stopCommand = commandBuilder.buildStopCommand(containerName, timeoutSeconds);
            Process process = executeCommand(stopCommand, timeoutSeconds + 10);
            process.waitFor(30, TimeUnit.SECONDS);
            log.info("Container stopped. container={}", containerName);
        } catch (Exception e) {
            log.warn("Failed to stop container gracefully, forcing removal. container={}", containerName, e);
            try {
                cleanupContainer(containerName);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 删除容器
     *
     * @param containerName 容器名称
     */
    public void removeContainer(String containerName) {
        try {
            List<String> rmCommand = commandBuilder.buildRmCommand(containerName, true);
            Process process = executeCommand(rmCommand, 30);
            process.waitFor(30, TimeUnit.SECONDS);
            log.info("Container removed. container={}", containerName);
        } catch (Exception e) {
            log.warn("Failed to remove container. container={}", containerName, e);
        }
    }

    /**
     * 清理容器 (停止 + 删除)
     *
     * @param containerName 容器名称
     */
    public void cleanupContainer(String containerName) {
        stopContainer(containerName, 10);
        removeContainer(containerName);
    }

    /**
     * 获取容器状态
     *
     * @param containerName 容器名称
     * @return 容器状态字符串
     */
    public String getContainerState(String containerName) {
        try {
            List<String> inspectCommand = commandBuilder.buildInspectCommand(containerName);
            Process process = executeCommand(inspectCommand, 30);

            String output = readProcessOutput(process);
            if (process.exitValue() != 0) {
                return "unknown";
            }

            JsonNode root = objectMapper.readTree(output);
            if (root.isArray() && root.size() > 0) {
                JsonNode state = root.get(0).get("State");
                if (state != null) {
                    if (state.has("Status")) {
                        return state.get("Status").asText();
                    }
                }
            }
            return "unknown";

        } catch (Exception e) {
            log.debug("Failed to get container state. container={}", containerName, e);
            return "unknown";
        }
    }

    /**
     * 检查 Docker 是否可用
     *
     * @return 是否可用
     */
    public boolean isDockerAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    dockerProperties.getBinary(),
                    "info",
                    "--format",
                    "{{.ServerVersion}}"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean completed = process.waitFor(10, TimeUnit.SECONDS);
            return completed && process.exitValue() == 0;
        } catch (Exception e) {
            log.warn("Docker is not available", e);
            return false;
        }
    }

    /**
     * 检查 gVisor runsc 运行时是否可用
     *
     * @return 是否可用
     */
    public boolean isGVisorAvailable() {
        try {
            // 检查 runsc 是否安装
            ProcessBuilder pb = new ProcessBuilder(
                    gVisorProperties.getRunscPath(),
                    "--version"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean completed = process.waitFor(10, TimeUnit.SECONDS);

            if (!completed || process.exitValue() != 0) {
                log.warn("runsc binary not found or not executable. path={}",
                        gVisorProperties.getRunscPath());
                return false;
            }

            // 检查 Docker 是否配置了 runsc 运行时
            pb = new ProcessBuilder(
                    dockerProperties.getBinary(),
                    "info",
                    "--format",
                    "{{.Runtimes}}"
            );
            pb.redirectErrorStream(true);
            process = pb.start();
            completed = process.waitFor(10, TimeUnit.SECONDS);

            if (!completed || process.exitValue() != 0) {
                return false;
            }

            String runtimes = readProcessOutput(process);
            return runtimes.contains(gVisorProperties.getRuntimeName());

        } catch (Exception e) {
            log.warn("Failed to check gVisor availability", e);
            return false;
        }
    }

    /**
     * 验证运行时环境
     *
     * @return 验证结果
     */
    public RuntimeCheckResult checkRuntime() {
        boolean dockerAvailable = isDockerAvailable();
        boolean gvisorAvailable = isGVisorAvailable();
        boolean sandboxEnabled = gVisorProperties.getEnabled();

        String status;
        if (!dockerAvailable) {
            status = "DOCKER_UNAVAILABLE";
        } else if (sandboxEnabled && !gvisorAvailable) {
            status = "GVISOR_UNAVAILABLE";
        } else if (sandboxEnabled && gvisorAvailable) {
            status = "READY_SANDBOXED";
        } else {
            status = "READY_UNSAFE";
        }

        return new RuntimeCheckResult(
                dockerAvailable,
                gvisorAvailable,
                sandboxEnabled,
                status,
                gVisorProperties.getRuntimeName(),
                gVisorProperties.getRunscPath()
        );
    }

    // ============ 私有辅助方法 ============

    private ContainerExecutionContext buildExecutionContext(
            String containerName,
            String imageName,
            int timeoutSeconds,
            WorkspaceManager.WorkspaceContext workspace
    ) {
        return ContainerExecutionContext.builder()
                .containerName(containerName)
                .imageName(imageName)
                .timeoutSeconds(timeoutSeconds)
                .inputDir(workspace.inputDir())
                .outputDir(workspace.outputDir())
                .logsDir(workspace.logsDir())
                .tempDir(workspace.tempDir())
                .stdoutPath(workspace.stdoutLog())
                .stderrPath(workspace.stderrLog())
                .resultJsonPath(workspace.resultJson())
                .gvisorSandboxEnabled(gVisorProperties.getEnabled())
                .gvisorSandboxType(gVisorProperties.getSandboxType())
                .gvisorTraceEnabled(gVisorProperties.getTraceSyscall())
                .build();
    }

    private String buildContainerName(String taskNo, Integer attemptNo) {
        String safeTaskNo = taskNo != null ? taskNo.replaceAll("[^a-zA-Z0-9]", "_") : "unknown";
        String attempt = attemptNo != null ? String.valueOf(attemptNo) : "0";
        return String.format("actrade_%s_attempt%s_%s",
                safeTaskNo, attempt, UUID.randomUUID().toString().substring(0, 8));
    }

    private String resolveImageName(ClaimTaskResponse claimResponse) {
        // 优先使用 claim response 中的镜像
        if (claimResponse.toolVersion() != null
                && claimResponse.toolVersion().runtimeOs() != null) {
            String os = claimResponse.toolVersion().runtimeOs();
            String arch = claimResponse.toolVersion().runtimeArch() != null
                    ? claimResponse.toolVersion().runtimeArch()
                    : "amd64";
            // 如果镜像名为空，构建一个
            if (claimResponse.packageAsset() != null
                    && claimResponse.packageAsset().storedFilename() != null) {
                String filename = claimResponse.packageAsset().storedFilename();
                return filename.replace(".zip", ":" + os + "-" + arch);
            }
        }
        // 默认使用配置中的 shell 镜像
        return dockerProperties.getImage().getShell();
    }

    private int resolveTimeout(ClaimTaskResponse claimResponse) {
        if (claimResponse.toolVersion() != null
                && claimResponse.toolVersion().timeoutSeconds() != null) {
            return claimResponse.toolVersion().timeoutSeconds();
        }
        return dockerProperties.getDefaultTimeoutSeconds();
    }

    private Map<String, String> parseEnvVars(String payloadJson) {
        // 简单实现：从 payload JSON 中提取环境变量
        // 实际实现可能需要更复杂的解析逻辑
        if (payloadJson == null || payloadJson.isBlank()) {
            return Map.of();
        }
        try {
            JsonNode payload = objectMapper.readTree(payloadJson);
            // 这里可以解析 payload 中的环境变量
            return Map.of();
        } catch (Exception e) {
            return Map.of();
        }
    }

    private Process executeCommand(List<String> command, int timeoutSeconds) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
        pb.redirectError(ProcessBuilder.Redirect.PIPE);

        Process process = pb.start();

        // 设置超时线程
        Thread timeoutThread = new Thread(() -> {
            try {
                Thread.sleep(timeoutSeconds * 1000L);
                if (process.isAlive()) {
                    log.debug("Process timed out, destroying. command={}", command.get(0));
                    process.destroyForcibly();
                }
            } catch (InterruptedException ignored) {
            }
        });
        timeoutThread.setDaemon(true);
        timeoutThread.start();

        return process;
    }

    private String readProcessOutput(Process process) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (var reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append(System.lineSeparator());
            }
        }
        return sb.toString();
    }

    private String extractContainerId(String output) {
        // docker create 输出容器 ID (可能是短 ID 或完整 ID)
        if (output == null || output.isBlank()) {
            return null;
        }
        String trimmed = output.trim();
        // 返回完整 ID 或短 ID
        if (trimmed.length() >= 12) {
            return trimmed.substring(0, 12);
        }
        return trimmed;
    }

    private Map<String, String> collectContainerStats(String containerId) {
        // 采集容器性能统计
        // 实际实现可能需要调用 docker stats 命令
        return Map.of();
    }

    /**
     * 运行时检查结果
     *
     * @param dockerAvailable Docker 是否可用
     * @param gvisorAvailable gVisor 是否可用
     * @param sandboxEnabled 沙箱是否启用
     * @param status 状态
     * @param runtimeName 运行时名称
     * @param runscPath runsc 路径
     */
    public record RuntimeCheckResult(
            boolean dockerAvailable,
            boolean gvisorAvailable,
            boolean sandboxEnabled,
            String status,
            String runtimeName,
            String runscPath
    ) {
        public boolean isReady() {
            return dockerAvailable && (!sandboxEnabled || gvisorAvailable);
        }
    }
}
