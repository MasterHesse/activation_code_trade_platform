package com.masterhesse.activation.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.masterhesse.activation.api.internal.request.ClaimTaskRequest;
import com.masterhesse.activation.api.internal.request.FinishTaskRequest;
import com.masterhesse.activation.api.internal.response.ClaimTaskResponse;
import com.masterhesse.activation.api.internal.response.FinishTaskResponse;
import com.masterhesse.activation.api.response.ActivationToolVersionResponse;
import com.masterhesse.activation.api.response.FileAssetResponse;
import com.masterhesse.activation.domain.entity.ActivationTask;
import com.masterhesse.activation.domain.entity.ActivationToolVersion;
import com.masterhesse.activation.domain.entity.FileAsset;
import com.masterhesse.activation.domain.enums.ActivationTaskStatus;
import com.masterhesse.activation.persistence.ActivationTaskRepository;
import com.masterhesse.activation.persistence.ActivationToolVersionRepository;
import com.masterhesse.activation.persistence.FileAssetRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class InternalActivationTaskService {

    private final ActivationTaskRepository activationTaskRepository;
    private final ActivationToolVersionRepository activationToolVersionRepository;
    private final FileAssetRepository fileAssetRepository;
    private final ObjectMapper objectMapper;

    public InternalActivationTaskService(
            ActivationTaskRepository activationTaskRepository,
            ActivationToolVersionRepository activationToolVersionRepository,
            FileAssetRepository fileAssetRepository,
            ObjectMapper objectMapper
    ) {
        this.activationTaskRepository = activationTaskRepository;
        this.activationToolVersionRepository = activationToolVersionRepository;
        this.fileAssetRepository = fileAssetRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ClaimTaskResponse claim(ClaimTaskRequest request) {
        Optional<ActivationTask> optionalTask = activationTaskRepository.findByTaskNoForUpdate(request.taskNo());
        if (optionalTask.isEmpty()) {
            log.warn("Claim ignored: task not found. taskNo={}, runnerInstanceId={}",
                    request.taskNo(), request.runnerInstanceId());
            return ClaimTaskResponse.notClaimed("TASK_NOT_FOUND");
        }

        ActivationTask task = optionalTask.get();

        if (!isClaimableStatus(task.getStatus())) {
            log.info("Claim ignored: task status not claimable. taskId={}, taskNo={}, status={}",
                    task.getId(), task.getTaskNo(), statusName(task.getStatus()));
            return ClaimTaskResponse.notClaimed("TASK_STATUS_NOT_CLAIMABLE");
        }

        int nextAttemptNo = safeAttemptCount(task) + 1;
        if (request.expectedAttemptNo() != null && !request.expectedAttemptNo().equals(nextAttemptNo)) {
            log.info("Claim ignored: expectedAttemptNo mismatch. taskId={}, taskNo={}, expected={}, actual={}",
                    task.getId(), task.getTaskNo(), request.expectedAttemptNo(), nextAttemptNo);
            return ClaimTaskResponse.notClaimed("ATTEMPT_MISMATCH");
        }

        ActivationToolVersion toolVersion = activationToolVersionRepository
                .findByToolVersionId(task.getActivationToolVersionId())
                .orElse(null);

        if (toolVersion == null) {
            markTaskFailedImmediately(task, "TOOL_VERSION_NOT_FOUND", "Activation tool version not found");
            return ClaimTaskResponse.notClaimed("TOOL_VERSION_NOT_FOUND");
        }

        FileAsset packageAsset = fileAssetRepository.findByFileId(toolVersion.getFileId()).orElse(null);
        if (packageAsset == null) {
            markTaskFailedImmediately(task, "PACKAGE_ASSET_NOT_FOUND", "Tool package asset not found");
            return ClaimTaskResponse.notClaimed("PACKAGE_ASSET_NOT_FOUND");
        }

        task.setAttemptCount(nextAttemptNo);
        task.setStatus(resolveStatus("RUNNING"));
        task.setStartedAt(LocalDateTime.now());
        task.setFinishedAt(null);
        task.setErrorCode(null);
        task.setErrorMessage(null);

        activationTaskRepository.save(task);

        log.info("Task claimed successfully. taskId={}, taskNo={}, attemptNo={}, runnerInstanceId={}, messageId={}",
                task.getId(), task.getTaskNo(), nextAttemptNo, request.runnerInstanceId(), request.messageId());

        return ClaimTaskResponse.claimed(
                task.getId(),
                task.getTaskNo(),
                nextAttemptNo,
                task.getMerchantId(),
                task.getActivationToolId(),
                task.getActivationToolVersionId(),
                task.getPayloadJson(),
                ActivationToolVersionResponse.from(toolVersion),
                FileAssetResponse.from(packageAsset)
        );
    }

    @Transactional
    public FinishTaskResponse finish(Long taskId, FinishTaskRequest request) {
        Optional<ActivationTask> optionalTask = activationTaskRepository.findByIdForUpdate(taskId);
        if (optionalTask.isEmpty()) {
            log.warn("Finish ignored as duplicate: task not found. taskId={}, runnerInstanceId={}",
                    taskId, request.runnerInstanceId());
            return FinishTaskResponse.duplicate(
                    "TASK_NOT_FOUND",
                    false,
                    null,
                    "task not found"
            );
        }

        ActivationTask task = optionalTask.get();
        String currentStatus = statusName(task.getStatus());

        if (!isRunningStatus(task.getStatus())) {
            log.info("Finish treated as duplicate/non-running. taskId={}, taskNo={}, status={}, runnerInstanceId={}",
                    task.getId(), task.getTaskNo(), currentStatus, request.runnerInstanceId());

            Integer nextAttemptNo = hasRemainingAttempts(task) ? safeAttemptCount(task) + 1 : null;
            boolean retryScheduled = isRetryLikeStatus(task.getStatus());

            return FinishTaskResponse.duplicate(
                    currentStatus,
                    retryScheduled,
                    nextAttemptNo,
                    "task is not in RUNNING status"
            );
        }

        task.setStartedAt(toLocalDateTime(request.startedAt(), task.getStartedAt()));
        task.setFinishedAt(toLocalDateTime(request.finishedAt(), LocalDateTime.now()));
        task.setResultSummaryJson(buildResultSummaryJson(request));

        if (Boolean.TRUE.equals(request.success())) {
            task.setStatus(resolveStatus("SUCCEEDED", "SUCCESS"));
            task.setErrorCode(null);
            task.setErrorMessage(null);

            activationTaskRepository.save(task);

            log.info("Task finished successfully. taskId={}, taskNo={}, attemptNo={}, runnerInstanceId={}",
                    task.getId(), task.getTaskNo(), task.getAttemptCount(), request.runnerInstanceId());

            return FinishTaskResponse.accepted(
                    statusName(task.getStatus()),
                    false,
                    null,
                    "task finished successfully"
            );
        }

        task.setErrorCode(nullIfBlank(request.errorCode()));
        task.setErrorMessage(truncate(nullIfBlank(request.errorMessage()), 4000));

        if (hasRemainingAttempts(task)) {
            int nextAttemptNo = safeAttemptCount(task) + 1;

            task.setStatus(resolveRetryStatus());
            task.setScheduledAt(nextRetryTime(task));

            activationTaskRepository.save(task);

            log.warn("Task finished with failure, retry scheduled. taskId={}, taskNo={}, currentAttemptNo={}, nextAttemptNo={}, runnerInstanceId={}",
                    task.getId(), task.getTaskNo(), task.getAttemptCount(), nextAttemptNo, request.runnerInstanceId());

            // TODO:
            // 1. 这里补 MQ 重新投递 / outbox 发布
            // 2. expectedAttemptNo = nextAttemptNo
            // 3. messageId 建议重新生成

            return FinishTaskResponse.accepted(
                    statusName(task.getStatus()),
                    true,
                    nextAttemptNo,
                    "task failed, retry scheduled"
            );
        }

        task.setStatus(resolveStatus("FAILED"));
        activationTaskRepository.save(task);

        log.warn("Task finished with failure, no retry left. taskId={}, taskNo={}, attemptNo={}, runnerInstanceId={}",
                task.getId(), task.getTaskNo(), task.getAttemptCount(), request.runnerInstanceId());

        return FinishTaskResponse.accepted(
                statusName(task.getStatus()),
                false,
                null,
                "task failed permanently"
        );
    }

    private void markTaskFailedImmediately(ActivationTask task, String errorCode, String errorMessage) {
        task.setStatus(resolveStatus("FAILED"));
        task.setFinishedAt(LocalDateTime.now());
        task.setErrorCode(errorCode);
        task.setErrorMessage(errorMessage);
        activationTaskRepository.save(task);

        log.error("Task marked failed immediately. taskId={}, taskNo={}, errorCode={}, errorMessage={}",
                task.getId(), task.getTaskNo(), errorCode, errorMessage);
    }

    private boolean isClaimableStatus(ActivationTaskStatus status) {
        if (status == null) {
            return false;
        }
        String name = status.name();
        return "PENDING".equals(name)
                || "QUEUED".equals(name)
                || "DISPATCHED".equals(name)
                || "RETRYING".equals(name)
                || "WAITING_RETRY".equals(name);
    }

    private boolean isRunningStatus(ActivationTaskStatus status) {
        return status != null && "RUNNING".equals(status.name());
    }

    private boolean isRetryLikeStatus(ActivationTaskStatus status) {
        if (status == null) {
            return false;
        }
        String name = status.name();
        return "PENDING".equals(name)
                || "QUEUED".equals(name)
                || "RETRYING".equals(name)
                || "WAITING_RETRY".equals(name);
    }

    private boolean hasRemainingAttempts(ActivationTask task) {
        int currentAttemptCount = safeAttemptCount(task);
        int maxAttempts = task.getMaxAttempts() == null || task.getMaxAttempts() < 1 ? 1 : task.getMaxAttempts();
        return currentAttemptCount < maxAttempts;
    }

    private int safeAttemptCount(ActivationTask task) {
        return task.getAttemptCount() == null ? 0 : task.getAttemptCount();
    }

    private String statusName(ActivationTaskStatus status) {
        return status == null ? "UNKNOWN" : status.name();
    }

    private ActivationTaskStatus resolveRetryStatus() {
        return resolveStatus("WAITING_RETRY", "RETRYING", "PENDING", "QUEUED");
    }

    private ActivationTaskStatus resolveStatus(String... candidates) {
        for (String candidate : candidates) {
            try {
                return Enum.valueOf(ActivationTaskStatus.class, candidate);
            } catch (IllegalArgumentException ignore) {
                // try next
            }
        }
        throw new IllegalStateException("No matching ActivationTaskStatus found for candidates: " + List.of(candidates));
    }

    private LocalDateTime nextRetryTime(ActivationTask task) {
        int currentAttempt = safeAttemptCount(task);
        long delaySeconds = Math.min(300L, Math.max(30L, currentAttempt * 30L));
        return LocalDateTime.now().plusSeconds(delaySeconds);
    }

    private LocalDateTime toLocalDateTime(OffsetDateTime value, LocalDateTime fallback) {
        return value == null ? fallback : value.toLocalDateTime();
    }

    private String nullIfBlank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String buildResultSummaryJson(FinishTaskRequest request) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("success", request.success());
        summary.put("summary", request.summary());
        summary.put("exitCode", request.exitCode());
        summary.put("errorCode", request.errorCode());
        summary.put("errorMessage", request.errorMessage());
        summary.put("startedAt", request.startedAt());
        summary.put("finishedAt", request.finishedAt());
        summary.put("durationMs", request.durationMs());
        summary.put("runnerInstanceId", request.runnerInstanceId());
        summary.put("timedOut", request.timedOut());
        summary.put("artifacts", request.artifacts());
        summary.put("diagnostics", request.diagnostics());

        try {
            return objectMapper.writeValueAsString(summary);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize result summary json. runnerInstanceId={}", request.runnerInstanceId(), ex);
            return "{\"serializeError\":true}";
        }
    }
}