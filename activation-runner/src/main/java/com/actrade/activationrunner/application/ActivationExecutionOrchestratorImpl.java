package com.actrade.activationrunner.application;

import com.actrade.activationrunner.client.ActradeInternalApiClient;
import com.actrade.activationrunner.client.dto.ArtifactItemDto;
import com.actrade.activationrunner.client.dto.ClaimTaskRequest;
import com.actrade.activationrunner.client.dto.ClaimTaskResponse;
import com.actrade.activationrunner.client.dto.FinishTaskRequest;
import com.actrade.activationrunner.client.dto.FinishTaskResponse;
import com.actrade.activationrunner.config.RunnerProperties;
import com.actrade.activationrunner.mq.ActivationTaskDispatchMessage;
import com.actrade.activationrunner.mq.MessageHandleResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivationExecutionOrchestratorImpl implements ActivationExecutionOrchestrator {

    private final ActradeInternalApiClient actradeInternalApiClient;
    private final WorkspaceManager workspaceManager;
    private final RunnerProperties runnerProperties;
    private final ObjectMapper objectMapper;
    private final PackageDownloadService packageDownloadService;
    private final DockerExecutionService dockerExecutionService;
    private final ArtifactUploadService artifactUploadService;

    @Override
    public MessageHandleResult handle(ActivationTaskDispatchMessage message) {
        ClaimTaskResponse claimResponse;
        try {
            claimResponse = actradeInternalApiClient.claimTask(new ClaimTaskRequest(
                    message.taskNo(),
                    runnerProperties.getInstanceId(),
                    message.expectedAttemptNo(),
                    message.messageId()
            ));
        } catch (Exception ex) {
            return MessageHandleResult.requeue("claim failed: " + ex.getClass().getSimpleName());
        }

        if (claimResponse == null) {
            return MessageHandleResult.requeue("claim response is null");
        }

        if (!claimResponse.claimed()) {
            log.info(
                    "Task claim skipped. taskId={}, taskNo={}, reason={}",
                    message.taskId(),
                    message.taskNo(),
                    claimResponse.reason()
            );
            return MessageHandleResult.ack("task not claimed: " + safeText(claimResponse.reason()));
        }

        if (!isValidClaimResponse(claimResponse)) {
            log.error(
                    "Claim response missing required fields. taskId={}, taskNo={}, attemptNo={}",
                    claimResponse.taskId(),
                    claimResponse.taskNo(),
                    claimResponse.attemptNo()
            );
            return MessageHandleResult.requeue("claim response incomplete");
        }

        WorkspaceManager.WorkspaceContext workspace = null;
        OffsetDateTime startedAt = OffsetDateTime.now();
        ExecutionOutcome outcome;

        try {
            workspace = workspaceManager.createWorkspace(claimResponse.taskNo(), claimResponse.attemptNo());
            outcome = runPackageDownloadExecution(claimResponse, workspace, startedAt);
        } catch (Exception ex) {
            log.error(
                    "Execution failed before finish callback. taskId={}, taskNo={}, attemptNo={}",
                    claimResponse.taskId(),
                    claimResponse.taskNo(),
                    claimResponse.attemptNo(),
                    ex
            );

            tryWriteFailureLogs(workspace, ex);

            outcome = buildFailureOutcome(startedAt, workspace, ex);
        }

        FinishTaskRequest finishRequest = toFinishTaskRequest(outcome);

        try {
            FinishTaskResponse finishResponse = actradeInternalApiClient.finishTask(claimResponse.taskId(), finishRequest);

            if (finishResponse == null) {
                return MessageHandleResult.requeue("finish response is null");
            }

            if (finishResponse.accepted() || finishResponse.duplicate()) {
                workspaceManager.cleanupIfEnabled(workspace);
                return MessageHandleResult.ack(
                        finishResponse.duplicate() ? "finish duplicate accepted" : "finish accepted"
                );
            }

            log.warn(
                    "Finish task not accepted. taskId={}, taskNo={}, taskStatus={}, retryScheduled={}, nextAttemptNo={}",
                    claimResponse.taskId(),
                    claimResponse.taskNo(),
                    finishResponse.taskStatus(),
                    finishResponse.retryScheduled(),
                    finishResponse.nextAttemptNo()
            );

            return MessageHandleResult.requeue("finish not accepted");
        } catch (Exception ex) {
            log.error(
                    "Finish callback failed, keep workspace for retry/debug. taskId={}, taskNo={}, workspace={}",
                    claimResponse.taskId(),
                    claimResponse.taskNo(),
                    workspace == null ? null : workspace.rootDir(),
                    ex
            );
            return MessageHandleResult.requeue("finish failed: " + ex.getClass().getSimpleName());
        }
    }

    private boolean isValidClaimResponse(ClaimTaskResponse response) {
        return response.taskId() != null
                && response.taskNo() != null
                && response.attemptNo() != null;
    }

    /**
     * 执行任务：下载、解压并运行容器
     *
     * <p>完整的执行流程：
     * <ol>
     *   <li>从 MinIO 下载工具包</li>
     *   <li>校验 SHA-256</li>
     *   <li>解压到工作目录</li>
     *   <li>在 gVisor 沙箱中执行 Docker 容器</li>
     *   <li>采集执行结果和日志</li>
     *   <li>清理容器资源</li>
     * </ol>
     * </p>
     *
     * <p>gVisor 集成说明：
     * <ul>
     *   <li>所有容器默认使用 runsc 运行时运行在 gVisor 沙箱中</li>
     *   <li>网络默认禁用，增强安全性</li>
     *   <li>支持系统调用跟踪用于调试</li>
     *   <li>支持内存和进程数限制</li>
     * </ul>
     * </p>
     */
    private ExecutionOutcome runPackageDownloadExecution(ClaimTaskResponse claimResponse,
                                                          WorkspaceManager.WorkspaceContext workspace,
                                                          OffsetDateTime startedAt) throws Exception {

        Map<String, String> diagnostics = new LinkedHashMap<>();
        diagnostics.put("mode", "docker-container");
        diagnostics.put("runnerInstanceId", runnerProperties.getInstanceId());
        diagnostics.put("workspaceRoot", workspace.rootDir().toString());
        diagnostics.put("taskNo", nullToEmpty(claimResponse.taskNo()));
        diagnostics.put("attemptNo", String.valueOf(claimResponse.attemptNo()));
        diagnostics.put("merchantId", claimResponse.merchantId() == null ? "" : String.valueOf(claimResponse.merchantId()));

        PackageDownloadResult downloadResult = null;
        ContainerExecutionResult containerResult = null;
        String errorCode = null;
        String errorMessage = null;

        // 步骤 1: 下载并解压工具包 (如果存在)
        if (claimResponse.packageAsset() != null) {
            log.info("Downloading package for task. taskId={}, taskNo={}, bucket={}, objectKey={}",
                    claimResponse.taskId(),
                    claimResponse.taskNo(),
                    claimResponse.packageAsset().bucketName(),
                    claimResponse.packageAsset().objectKey());

            downloadResult = packageDownloadService.downloadAndExtract(
                    claimResponse.packageAsset(),
                    workspace.inputDir()
            );

            if (downloadResult.success()) {
                diagnostics.put("packageFile", downloadResult.downloadedFile() != null
                        ? downloadResult.downloadedFile().toString() : "N/A");
                diagnostics.put("packageExtractedDir", downloadResult.extractedDir() != null
                        ? downloadResult.extractedDir().toString() : "N/A");
                diagnostics.put("packageChecksum", nullToEmpty(downloadResult.checksum()));
                diagnostics.put("packageSizeBytes", String.valueOf(downloadResult.fileSizeBytes()));
                log.info("Package downloaded successfully. taskNo={}, extractedDir={}",
                        claimResponse.taskNo(), downloadResult.extractedDir());
            } else {
                errorCode = downloadResult.errorCode() != null ? downloadResult.errorCode() : "DOWNLOAD_FAILED";
                errorMessage = downloadResult.errorMessage();
                diagnostics.put("downloadErrorCode", errorCode);
                diagnostics.put("downloadErrorMessage", nullToEmpty(errorMessage));
                log.error("Package download failed. taskNo={}, errorCode={}, errorMessage={}",
                        claimResponse.taskNo(), errorCode, errorMessage);
            }
        } else {
            diagnostics.put("message", "No package asset in claim response, skipping download");
            log.info("No package asset found in claim response. taskNo={}", claimResponse.taskNo());
        }

        // 步骤 2: 执行 Docker 容器 (gVisor 沙箱)
        if (errorCode == null) {  // 仅当下载成功时执行容器
            log.info("Executing container for task. taskId={}, taskNo={}",
                    claimResponse.taskId(), claimResponse.taskNo());

            containerResult = dockerExecutionService.executeContainerTask(
                    claimResponse.taskNo(),
                    claimResponse.attemptNo(),
                    claimResponse,
                    workspace
            );

            if (containerResult != null) {
                // 更新诊断信息
                diagnostics.put("containerId", nullToEmpty(containerResult.containerId()));
                diagnostics.put("containerName", nullToEmpty(containerResult.containerName()));
                diagnostics.put("containerState", nullToEmpty(containerResult.containerState()));
                diagnostics.put("exitCode", String.valueOf(containerResult.exitCode()));
                diagnostics.put("exitReason", nullToEmpty(containerResult.exitReason()));
                diagnostics.put("durationMs", String.valueOf(containerResult.durationMs()));
                diagnostics.put("timedOut", String.valueOf(containerResult.timedOut()));

                // gVisor 信息
                diagnostics.put("gvisorEnabled", String.valueOf(containerResult.gvisorSandboxEnabled()));
                diagnostics.put("gvisorType", nullToEmpty(containerResult.gvisorSandboxType()));

                if (containerResult.memoryUsageMbPeak() != null) {
                    diagnostics.put("memoryUsageMbPeak", String.valueOf(containerResult.memoryUsageMbPeak()));
                }

                if (!containerResult.success()) {
                    errorCode = containerResult.errorCode() != null
                            ? containerResult.errorCode()
                            : (containerResult.timedOut() ? "EXECUTION_TIMEOUT" : "EXECUTION_FAILED");
                    errorMessage = containerResult.errorMessage();
                }

                log.info("Container execution completed. taskNo={}, success={}, exitCode={}, duration={}ms",
                        claimResponse.taskNo(),
                        containerResult.success(),
                        containerResult.exitCode(),
                        containerResult.durationMs());
            }
        }

        // 步骤 3: 生成执行信息文件
        String stdout = buildStdoutContent(claimResponse, workspace, downloadResult, containerResult);
        String stderr = buildStderrContent(containerResult);

        workspaceManager.writeUtf8(workspace.stdoutLog(), stdout);
        workspaceManager.writeUtf8(workspace.stderrLog(), stderr);
        workspaceManager.writeUtf8(workspace.resultJson(),
                buildResultJson(claimResponse, workspace, diagnostics, downloadResult, containerResult));

        // 步骤 4: 收集并上传执行产物
        List<ArtifactItemDto> artifacts = List.of();
        if (containerResult != null && containerResult.success()) {
            try {
                ArtifactUploadResult uploadResult = artifactUploadService.collectAndUploadArtifacts(
                        workspace, claimResponse.taskNo(), claimResponse.attemptNo());
                artifacts = uploadResult.artifacts();
                diagnostics.put("artifactCount", String.valueOf(artifacts.size()));
                diagnostics.put("artifactUploadSuccess", String.valueOf(uploadResult.success()));
                diagnostics.put("artifactUploadBytes", String.valueOf(uploadResult.totalSizeBytes()));
                if (!uploadResult.success()) {
                    diagnostics.put("artifactUploadErrorCode", nullToEmpty(uploadResult.errorCode()));
                    diagnostics.put("artifactUploadErrorMessage", nullToEmpty(uploadResult.errorMessage()));
                }
                log.info("Artifact upload completed. taskNo={}, count={}, success={}",
                        claimResponse.taskNo(), artifacts.size(), uploadResult.success());
            } catch (Exception uploadEx) {
                log.warn("Artifact upload failed, continuing with empty artifacts. taskNo={}",
                        claimResponse.taskNo(), uploadEx);
                diagnostics.put("artifactUploadError", uploadEx.getClass().getSimpleName());
                diagnostics.put("artifactUploadErrorMessage", safeText(uploadEx.getMessage()));
            }
        } else {
            diagnostics.put("artifactUploadSkipped", "execution_not_successful");
        }

        OffsetDateTime finishedAt = OffsetDateTime.now();
        long durationMs = Duration.between(startedAt, finishedAt).toMillis();

        // 判断是否成功
        boolean containerSuccess = containerResult == null || containerResult.success();
        boolean downloadSuccess = downloadResult == null || downloadResult.success();
        boolean success = downloadSuccess && containerSuccess;

        return new ExecutionOutcome(
                success,
                success ? "EXECUTION_OK" : "EXECUTION_FAILED",
                containerResult != null ? containerResult.exitCode() : (success ? 0 : -1),
                errorCode,
                errorMessage,
                startedAt,
                finishedAt,
                durationMs,
                containerResult != null && containerResult.timedOut(),
                artifacts,
                diagnostics
        );
    }

    private ExecutionOutcome buildFailureOutcome(OffsetDateTime startedAt,
                                                 WorkspaceManager.WorkspaceContext workspace,
                                                 Exception ex) {
        OffsetDateTime finishedAt = OffsetDateTime.now();
        long durationMs = Duration.between(startedAt, finishedAt).toMillis();

        Map<String, String> diagnostics = new LinkedHashMap<>();
        diagnostics.put("mode", "skeleton");
        diagnostics.put("runnerInstanceId", runnerProperties.getInstanceId());
        diagnostics.put("workspaceRoot", workspace == null ? "" : workspace.rootDir().toString());
        diagnostics.put("exceptionType", ex.getClass().getName());
        diagnostics.put("message", safeText(ex.getMessage()));

        return new ExecutionOutcome(
                false,
                "SKELETON_FAILED",
                -1,
                "RUNNER_EXECUTION_ERROR",
                truncate(stackTraceOf(ex), 4000),
                startedAt,
                finishedAt,
                durationMs,
                false,
                List.of(),
                diagnostics
        );
    }

    private FinishTaskRequest toFinishTaskRequest(ExecutionOutcome outcome) {
        return new FinishTaskRequest(
                outcome.success(),
                outcome.summary(),
                outcome.exitCode(),
                outcome.errorCode(),
                outcome.errorMessage(),
                outcome.startedAt(),
                outcome.finishedAt(),
                outcome.durationMs(),
                runnerProperties.getInstanceId(),
                outcome.timedOut(),
                outcome.artifacts(),
                outcome.diagnostics()
        );
    }

    private void tryWriteFailureLogs(WorkspaceManager.WorkspaceContext workspace, Exception ex) {
        if (workspace == null) {
            return;
        }

        try {
            workspaceManager.writeUtf8(
                    workspace.stderrLog(),
                    stackTraceOf(ex)
            );
        } catch (Exception writeEx) {
            log.warn("Failed to write failure stderr log. workspace={}", workspace.rootDir(), writeEx);
        }
    }

    private String buildStdoutContent(ClaimTaskResponse claimResponse,
                                       WorkspaceManager.WorkspaceContext workspace,
                                       PackageDownloadResult downloadResult,
                                       ContainerExecutionResult containerResult) {
        StringBuilder sb = new StringBuilder();
        sb.append("Activation runner execution").append(System.lineSeparator());
        sb.append("runnerInstanceId=").append(runnerProperties.getInstanceId()).append(System.lineSeparator());
        sb.append("taskId=").append(claimResponse.taskId()).append(System.lineSeparator());
        sb.append("taskNo=").append(claimResponse.taskNo()).append(System.lineSeparator());
        sb.append("attemptNo=").append(claimResponse.attemptNo()).append(System.lineSeparator());
        sb.append("merchantId=").append(claimResponse.merchantId()).append(System.lineSeparator());
        sb.append("workspace=").append(workspace.rootDir()).append(System.lineSeparator());

        // Package download info
        if (claimResponse.packageAsset() != null) {
            sb.append("packageBucket=").append(claimResponse.packageAsset().bucketName()).append(System.lineSeparator());
            sb.append("packageObjectKey=").append(claimResponse.packageAsset().objectKey()).append(System.lineSeparator());
            sb.append("packageChecksum=").append(claimResponse.packageAsset().checksumSha256()).append(System.lineSeparator());
        }

        if (downloadResult != null) {
            sb.append("packageDownloadSuccess=").append(downloadResult.success()).append(System.lineSeparator());
            if (downloadResult.success()) {
                sb.append("packageExtractedDir=").append(downloadResult.extractedDir()).append(System.lineSeparator());
            } else {
                sb.append("packageDownloadError=").append(downloadResult.errorCode())
                        .append(": ").append(downloadResult.errorMessage()).append(System.lineSeparator());
            }
        }

        // Container execution info (gVisor sandbox)
        if (containerResult != null) {
            sb.append("--- Container Execution (gVisor Sandbox) ---").append(System.lineSeparator());
            sb.append("containerId=").append(nullToEmpty(containerResult.containerId())).append(System.lineSeparator());
            sb.append("containerName=").append(nullToEmpty(containerResult.containerName())).append(System.lineSeparator());
            sb.append("containerState=").append(nullToEmpty(containerResult.containerState())).append(System.lineSeparator());
            sb.append("exitCode=").append(containerResult.exitCode()).append(System.lineSeparator());
            sb.append("exitReason=").append(nullToEmpty(containerResult.exitReason())).append(System.lineSeparator());
            sb.append("durationMs=").append(containerResult.durationMs()).append(System.lineSeparator());
            sb.append("timedOut=").append(containerResult.timedOut()).append(System.lineSeparator());
            sb.append("gvisorEnabled=").append(containerResult.gvisorSandboxEnabled()).append(System.lineSeparator());
            sb.append("gvisorType=").append(nullToEmpty(containerResult.gvisorSandboxType())).append(System.lineSeparator());
            sb.append("gvisorTraceEnabled=").append(containerResult.gvisorTraceEnabled()).append(System.lineSeparator());

            if (containerResult.memoryUsageMbPeak() != null) {
                sb.append("memoryUsageMbPeak=").append(containerResult.memoryUsageMbPeak()).append(System.lineSeparator());
            }
            if (containerResult.cpuUsagePercent() != null) {
                sb.append("cpuUsagePercent=").append(containerResult.cpuUsagePercent()).append(System.lineSeparator());
            }
        }

        if (claimResponse.toolVersion() != null) {
            sb.append("--- Tool Version ---").append(System.lineSeparator());
            sb.append("runtimeType=").append(claimResponse.toolVersion().runtimeType()).append(System.lineSeparator());
            sb.append("runtimeOs=").append(claimResponse.toolVersion().runtimeOs()).append(System.lineSeparator());
            sb.append("runtimeArch=").append(claimResponse.toolVersion().runtimeArch()).append(System.lineSeparator());
            sb.append("entrypoint=").append(claimResponse.toolVersion().entrypoint()).append(System.lineSeparator());
            sb.append("execCommand=").append(claimResponse.toolVersion().execCommand()).append(System.lineSeparator());
        }

        sb.append("payloadJson=").append(nullToEmpty(claimResponse.payloadJson())).append(System.lineSeparator());
        return sb.toString();
    }

    private String buildStderrContent(ContainerExecutionResult containerResult) {
        if (containerResult == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (!containerResult.success()) {
            sb.append("Container execution failed").append(System.lineSeparator());
            if (containerResult.errorCode() != null) {
                sb.append("errorCode=").append(containerResult.errorCode()).append(System.lineSeparator());
            }
            if (containerResult.errorMessage() != null) {
                sb.append("errorMessage=").append(containerResult.errorMessage()).append(System.lineSeparator());
            }
            if (containerResult.timedOut()) {
                sb.append("note=Execution timed out").append(System.lineSeparator());
            }
        }
        return sb.toString();
    }

    private String buildResultJson(ClaimTaskResponse claimResponse,
                                   WorkspaceManager.WorkspaceContext workspace,
                                   Map<String, String> diagnostics,
                                   PackageDownloadResult downloadResult,
                                   ContainerExecutionResult containerResult) throws JsonProcessingException {

        Map<String, Object> result = new LinkedHashMap<>();

        // 确定状态
        boolean containerSuccess = containerResult == null || containerResult.success();
        boolean downloadSuccess = downloadResult == null || downloadResult.success();
        boolean overallSuccess = downloadSuccess && containerSuccess;

        result.put("status", overallSuccess ? "SUCCESS" : "FAILED");
        result.put("mode", "DOCKER_CONTAINER_GVISOR");
        result.put("taskId", claimResponse.taskId());
        result.put("taskNo", claimResponse.taskNo());
        result.put("attemptNo", claimResponse.attemptNo());
        result.put("merchantId", claimResponse.merchantId());
        result.put("workspaceRoot", workspace.rootDir().toString());
        result.put("diagnostics", diagnostics);

        // 添加下载结果信息
        if (downloadResult != null) {
            Map<String, Object> downloadInfo = new LinkedHashMap<>();
            downloadInfo.put("success", downloadResult.success());
            downloadInfo.put("checksum", downloadResult.checksum());
            downloadInfo.put("fileSizeBytes", downloadResult.fileSizeBytes());
            if (downloadResult.success()) {
                downloadInfo.put("extractedDir", downloadResult.extractedDir() != null
                        ? downloadResult.extractedDir().toString() : null);
            } else {
                downloadInfo.put("errorCode", downloadResult.errorCode());
                downloadInfo.put("errorMessage", downloadResult.errorMessage());
            }
            result.put("downloadResult", downloadInfo);
        }

        // 添加容器执行结果 (gVisor sandbox)
        if (containerResult != null) {
            Map<String, Object> containerInfo = new LinkedHashMap<>();
            containerInfo.put("success", containerResult.success());
            containerInfo.put("containerId", containerResult.containerId());
            containerInfo.put("containerName", containerResult.containerName());
            containerInfo.put("imageName", containerResult.imageName());
            containerInfo.put("containerState", containerResult.containerState());
            containerInfo.put("exitCode", containerResult.exitCode());
            containerInfo.put("exitReason", containerResult.exitReason());
            containerInfo.put("durationMs", containerResult.durationMs());
            containerInfo.put("timedOut", containerResult.timedOut());
            containerInfo.put("timeoutSeconds", containerResult.timeoutSeconds());

            // gVisor sandbox info
            Map<String, Object> gvisorInfo = new LinkedHashMap<>();
            gvisorInfo.put("enabled", containerResult.gvisorSandboxEnabled());
            gvisorInfo.put("sandboxType", containerResult.gvisorSandboxType());
            gvisorInfo.put("syscallTraceEnabled", containerResult.gvisorTraceEnabled());
            containerInfo.put("gvisor", gvisorInfo);

            // Performance metrics
            if (containerResult.memoryUsageMbPeak() != null) {
                containerInfo.put("memoryUsageMbPeak", containerResult.memoryUsageMbPeak());
            }
            if (containerResult.cpuUsagePercent() != null) {
                containerInfo.put("cpuUsagePercent", containerResult.cpuUsagePercent());
            }

            if (!containerResult.success()) {
                containerInfo.put("errorCode", containerResult.errorCode());
                containerInfo.put("errorMessage", containerResult.errorMessage());
            }

            result.put("containerExecution", containerInfo);
        }

        if (claimResponse.toolVersion() != null) {
            Map<String, Object> toolVersion = new LinkedHashMap<>();
            toolVersion.put("versionName", claimResponse.toolVersion().versionName());
            toolVersion.put("runtimeType", claimResponse.toolVersion().runtimeType());
            toolVersion.put("runtimeOs", claimResponse.toolVersion().runtimeOs());
            toolVersion.put("runtimeArch", claimResponse.toolVersion().runtimeArch());
            toolVersion.put("entrypoint", claimResponse.toolVersion().entrypoint());
            toolVersion.put("execCommand", claimResponse.toolVersion().execCommand());
            toolVersion.put("timeoutSeconds", claimResponse.toolVersion().timeoutSeconds());
            toolVersion.put("maxMemoryMb", claimResponse.toolVersion().maxMemoryMb());
            result.put("toolVersion", toolVersion);
        }

        if (claimResponse.packageAsset() != null) {
            Map<String, Object> packageAsset = new LinkedHashMap<>();
            packageAsset.put("fileId", claimResponse.packageAsset().fileId());
            packageAsset.put("bucketName", claimResponse.packageAsset().bucketName());
            packageAsset.put("objectKey", claimResponse.packageAsset().objectKey());
            packageAsset.put("originalFilename", claimResponse.packageAsset().originalFilename());
            packageAsset.put("storedFilename", claimResponse.packageAsset().storedFilename());
            packageAsset.put("checksumSha256", claimResponse.packageAsset().checksumSha256());
            packageAsset.put("fileSizeBytes", claimResponse.packageAsset().fileSizeBytes());
            result.put("packageAsset", packageAsset);
        }

        if (claimResponse.payloadJson() != null && !claimResponse.payloadJson().isBlank()) {
            try {
                result.put("payload", objectMapper.readTree(claimResponse.payloadJson()));
            } catch (Exception ignore) {
                result.put("payloadRaw", claimResponse.payloadJson());
            }
        }

        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
    }

    private String stackTraceOf(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    private String safeText(String text) {
        return text == null ? "" : truncate(text, 500);
    }

    private String nullToEmpty(String text) {
        return text == null ? "" : text;
    }

    private record ExecutionOutcome(
            Boolean success,
            String summary,
            Integer exitCode,
            String errorCode,
            String errorMessage,
            OffsetDateTime startedAt,
            OffsetDateTime finishedAt,
            Long durationMs,
            Boolean timedOut,
            List<ArtifactItemDto> artifacts,
            Map<String, String> diagnostics
    ) {
    }
}