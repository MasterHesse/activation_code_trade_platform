package com.masterhesse.activation.domain.entity;

import com.masterhesse.activation.domain.enums.ActivationTaskAttemptStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "activation_task_attempt",
        indexes = {
                @Index(name = "idx_activation_task_attempt_task_id", columnList = "task_id"),
                @Index(name = "idx_activation_task_attempt_status", columnList = "status"),
                @Index(name = "idx_activation_task_attempt_runner_instance_id", columnList = "runner_instance_id"),
                @Index(name = "idx_activation_task_attempt_created_at", columnList = "created_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_activation_task_attempt_task_id_attempt_no", columnNames = {"task_id", "attempt_no"})
        }
)
public class ActivationTaskAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "attempt_no", nullable = false)
    private Integer attemptNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ActivationTaskAttemptStatus status;

    @Column(name = "runner_instance_id", length = 128)
    private String runnerInstanceId;

    @Column(name = "runner_node", length = 128)
    private String runnerNode;

    @Column(name = "sandbox_runtime", length = 64)
    private String sandboxRuntime;

    @Column(name = "container_id", length = 128)
    private String containerId;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "exit_code")
    private Integer exitCode;

    @Column(name = "timeout_seconds")
    private Integer timeoutSeconds;

    @Column(name = "error_code", length = 64)
    private String errorCode;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Lob
    @Column(name = "raw_result_json", columnDefinition = "TEXT")
    private String rawResultJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.status == null) {
            this.status = ActivationTaskAttemptStatus.CREATED;
        }
        if (this.attemptNo == null) {
            this.attemptNo = 1;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public ActivationTaskAttempt() {
    }

    public Long getId() {
        return id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Integer getAttemptNo() {
        return attemptNo;
    }

    public void setAttemptNo(Integer attemptNo) {
        this.attemptNo = attemptNo;
    }

    public ActivationTaskAttemptStatus getStatus() {
        return status;
    }

    public void setStatus(ActivationTaskAttemptStatus status) {
        this.status = status;
    }

    public String getRunnerInstanceId() {
        return runnerInstanceId;
    }

    public void setRunnerInstanceId(String runnerInstanceId) {
        this.runnerInstanceId = runnerInstanceId;
    }

    public String getRunnerNode() {
        return runnerNode;
    }

    public void setRunnerNode(String runnerNode) {
        this.runnerNode = runnerNode;
    }

    public String getSandboxRuntime() {
        return sandboxRuntime;
    }

    public void setSandboxRuntime(String sandboxRuntime) {
        this.sandboxRuntime = sandboxRuntime;
    }

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public Integer getExitCode() {
        return exitCode;
    }

    public void setExitCode(Integer exitCode) {
        this.exitCode = exitCode;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getRawResultJson() {
        return rawResultJson;
    }

    public void setRawResultJson(String rawResultJson) {
        this.rawResultJson = rawResultJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}