package com.masterhesse.activation.application;

import com.masterhesse.activation.domain.entity.ActivationTask;
import com.masterhesse.activation.domain.entity.ActivationTaskArtifact;
import com.masterhesse.activation.domain.entity.ActivationTaskAttempt;
import com.masterhesse.activation.domain.enums.ActivationTaskAttemptStatus;
import com.masterhesse.activation.domain.enums.ActivationTaskStatus;
import com.masterhesse.activation.persistence.ActivationTaskArtifactRepository;
import com.masterhesse.activation.persistence.ActivationTaskAttemptRepository;
import com.masterhesse.activation.persistence.ActivationTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class ActivationTaskRunner {

    private static final Logger log = LoggerFactory.getLogger(ActivationTaskRunner.class);

    private final ActivationTaskRepository activationTaskRepository;
    private final ActivationTaskAttemptRepository activationTaskAttemptRepository;
    private final ActivationTaskArtifactRepository activationTaskArtifactRepository;
    private final ActivationTaskExecutor activationTaskExecutor;
    private final TransactionTemplate transactionTemplate;

    public ActivationTaskRunner(ActivationTaskRepository activationTaskRepository,
                                ActivationTaskAttemptRepository activationTaskAttemptRepository,
                                ActivationTaskArtifactRepository activationTaskArtifactRepository,
                                ActivationTaskExecutor activationTaskExecutor,
                                PlatformTransactionManager transactionManager) {
        this.activationTaskRepository = activationTaskRepository;
        this.activationTaskAttemptRepository = activationTaskAttemptRepository;
        this.activationTaskArtifactRepository = activationTaskArtifactRepository;
        this.activationTaskExecutor = activationTaskExecutor;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public void run(Long taskId) {
        RunContext context = transactionTemplate.execute(status -> startAttempt(taskId));
        if (context == null) {
            return;
        }

        ActivationTaskExecutionResult result;
        try {
            result = activationTaskExecutor.execute(
                    context.getTaskId(),
                    context.getTaskNo(),
                    context.getAttemptId(),
                    context.getActivationToolVersionId(),
                    context.getPayloadJson()
            );

            if (result == null) {
                result = ActivationTaskExecutionResult.failed(
                        "EMPTY_EXECUTION_RESULT",
                        "ActivationTaskExecutor returned null"
                );
            }
        } catch (Exception e) {
            log.error(
                    "Unhandled exception while executing activation task. taskId={}, taskNo={}, attemptId={}",
                    context.getTaskId(),
                    context.getTaskNo(),
                    context.getAttemptId(),
                    e
            );

            result = ActivationTaskExecutionResult.failed(
                    "EXECUTION_EXCEPTION",
                    safeMessage(e)
            );
        }

        ActivationTaskExecutionResult finalResult = result;
        transactionTemplate.executeWithoutResult(status -> finishAttempt(context, finalResult));
    }

    private RunContext startAttempt(Long taskId) {
        ActivationTask task = activationTaskRepository.findByIdForUpdate(taskId).orElse(null);
        if (task == null) {
            log.warn("Skip execution because task not found. taskId={}", taskId);
            return null;
        }

        if (task.getStatus() != ActivationTaskStatus.PENDING) {
            log.info(
                    "Skip execution because task is not pending. taskId={}, taskNo={}, status={}",
                    task.getId(),
                    task.getTaskNo(),
                    task.getStatus()
            );
            return null;
        }

        Integer currentAttemptCount = task.getAttemptCount() == null ? 0 : task.getAttemptCount();
        Integer maxAttempts = task.getMaxAttempts();

        if (maxAttempts != null && currentAttemptCount >= maxAttempts) {
            LocalDateTime now = LocalDateTime.now();
            task.setStatus(ActivationTaskStatus.FAILED);
            task.setFinishedAt(now);
            task.setErrorCode("MAX_ATTEMPTS_EXCEEDED");
            task.setErrorMessage("Task cannot start because max attempts has been reached");
            activationTaskRepository.save(task);

            log.warn(
                    "Task cannot start because max attempts has been reached. taskId={}, taskNo={}, attemptCount={}, maxAttempts={}",
                    task.getId(),
                    task.getTaskNo(),
                    currentAttemptCount,
                    maxAttempts
            );
            return null;
        }

        LocalDateTime now = LocalDateTime.now();

        if (task.getScheduledAt() != null && task.getScheduledAt().isAfter(now)) {
            log.warn(
                    "Task scheduledAt is in the future, but minimal B-4 runner executes immediately. taskId={}, taskNo={}, scheduledAt={}",
                    task.getId(),
                    task.getTaskNo(),
                    task.getScheduledAt()
            );
        }

        int nextAttemptNo = currentAttemptCount + 1;

        task.setStatus(ActivationTaskStatus.RUNNING);
        task.setAttemptCount(nextAttemptNo);
        if (task.getStartedAt() == null) {
            task.setStartedAt(now);
        }
        task.setFinishedAt(null);
        task.setErrorCode(null);
        task.setErrorMessage(null);
        activationTaskRepository.save(task);

        ActivationTaskAttempt attempt = new ActivationTaskAttempt();
        attempt.setTaskId(task.getId());
        attempt.setAttemptNo(nextAttemptNo);
        attempt.setStatus(ActivationTaskAttemptStatus.RUNNING);
        attempt.setStartedAt(now);
        attempt.setFinishedAt(null);
        attempt.setErrorCode(null);
        attempt.setErrorMessage(null);

        activationTaskAttemptRepository.save(attempt);

        task.setLastAttemptId(attempt.getId());
        activationTaskRepository.save(task);

        log.info(
                "Activation task started. taskId={}, taskNo={}, attemptId={}, attemptNo={}",
                task.getId(),
                task.getTaskNo(),
                attempt.getId(),
                nextAttemptNo
        );

        return new RunContext(
                task.getId(),
                task.getTaskNo(),
                attempt.getId(),
                task.getActivationToolVersionId(),
                task.getPayloadJson()
        );
    }

    private void finishAttempt(RunContext context, ActivationTaskExecutionResult result) {
        ActivationTask task = activationTaskRepository.findByIdForUpdate(context.getTaskId()).orElse(null);
        ActivationTaskAttempt attempt = activationTaskAttemptRepository.findById(context.getAttemptId()).orElse(null);

        if (task == null) {
            log.error(
                    "Failed to finish activation task because task not found. taskId={}, attemptId={}",
                    context.getTaskId(),
                    context.getAttemptId()
            );
            return;
        }

        if (attempt == null) {
            log.error(
                    "Failed to finish activation task because attempt not found. taskId={}, attemptId={}",
                    context.getTaskId(),
                    context.getAttemptId()
            );
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        saveArtifacts(task.getId(), attempt.getId(), result.getArtifacts(), now);

        attempt.setFinishedAt(now);

        if (result.isSuccess()) {
            attempt.setStatus(ActivationTaskAttemptStatus.SUCCEEDED);
            attempt.setErrorCode(null);
            attempt.setErrorMessage(null);
            activationTaskAttemptRepository.save(attempt);

            task.setStatus(ActivationTaskStatus.SUCCEEDED);
            task.setFinishedAt(now);
            task.setErrorCode(null);
            task.setErrorMessage(null);
            activationTaskRepository.save(task);

            log.info(
                    "Activation task finished successfully. taskId={}, taskNo={}, attemptId={}, summary={}, exitCode={}",
                    task.getId(),
                    task.getTaskNo(),
                    attempt.getId(),
                    result.getSummary(),
                    result.getExitCode()
            );
            return;
        }

        attempt.setStatus(ActivationTaskAttemptStatus.FAILED);
        attempt.setErrorCode(result.getErrorCode());
        attempt.setErrorMessage(result.getErrorMessage());
        activationTaskAttemptRepository.save(attempt);

        task.setStatus(ActivationTaskStatus.FAILED);
        task.setFinishedAt(now);
        task.setErrorCode(result.getErrorCode());
        task.setErrorMessage(result.getErrorMessage());
        activationTaskRepository.save(task);

        log.warn(
                "Activation task finished with failure. taskId={}, taskNo={}, attemptId={}, errorCode={}, errorMessage={}, exitCode={}",
                task.getId(),
                task.getTaskNo(),
                attempt.getId(),
                result.getErrorCode(),
                result.getErrorMessage(),
                result.getExitCode()
        );
    }

    private void saveArtifacts(Long taskId,
                               Long attemptId,
                               List<ActivationTaskExecutionArtifact> artifacts,
                               LocalDateTime createdAt) {
        if (CollectionUtils.isEmpty(artifacts)) {
            return;
        }

        for (ActivationTaskExecutionArtifact artifact : artifacts) {
            ActivationTaskArtifact entity = new ActivationTaskArtifact();
            entity.setTaskId(taskId);
            entity.setAttemptId(attemptId);
            entity.setArtifactType(artifact.getArtifactType());
            entity.setFileName(artifact.getFileName());
            entity.setFilePath(artifact.getFilePath());
            entity.setMediaType(artifact.getMediaType());
            entity.setFileSize(artifact.getFileSize());
            entity.setCreatedAt(createdAt);
            activationTaskArtifactRepository.save(entity);
        }
    }

    private String safeMessage(Throwable throwable) {
        if (throwable == null) {
            return "unknown";
        }
        if (StringUtils.hasText(throwable.getMessage())) {
            return throwable.getMessage();
        }
        return throwable.getClass().getSimpleName();
    }

    private static class RunContext {

        private final Long taskId;
        private final String taskNo;
        private final Long attemptId;
        private final UUID activationToolVersionId;
        private final String payloadJson;

        public RunContext(Long taskId,
                          String taskNo,
                          Long attemptId,
                          UUID activationToolVersionId,
                          String payloadJson) {
            this.taskId = taskId;
            this.taskNo = taskNo;
            this.attemptId = attemptId;
            this.activationToolVersionId = activationToolVersionId;
            this.payloadJson = payloadJson;
        }

        public Long getTaskId() {
            return taskId;
        }

        public String getTaskNo() {
            return taskNo;
        }

        public Long getAttemptId() {
            return attemptId;
        }

        public UUID getActivationToolVersionId() {
            return activationToolVersionId;
        }

        public String getPayloadJson() {
            return payloadJson;
        }
    }

}