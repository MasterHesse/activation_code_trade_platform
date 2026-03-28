package com.masterhesse.activation.domain.entity;

import com.masterhesse.activation.domain.enums.ActivationTaskSourceType;
import com.masterhesse.activation.domain.enums.ActivationTaskStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "activation_task",
        indexes = {
                @Index(name = "idx_activation_task_status", columnList = "status"),
                @Index(name = "idx_activation_task_order_id", columnList = "order_id"),
                @Index(name = "idx_activation_task_order_item_id", columnList = "order_item_id"),
                @Index(name = "idx_activation_task_merchant_id", columnList = "merchant_id"),
                @Index(name = "idx_activation_task_tool_version_id", columnList = "activation_tool_version_id"),
                @Index(name = "idx_activation_task_created_at", columnList = "created_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_activation_task_task_no", columnNames = "task_no")
        }
)
public class ActivationTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_no", nullable = false, length = 64)
    private String taskNo;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "order_item_id")
    private Long orderItemId;

    @Column(name = "merchant_id")
    private Long merchantId;

    @Column(name = "activation_tool_id")
    private UUID activationToolId;

    @Column(name = "activation_tool_version_id")
    private UUID activationToolVersionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ActivationTaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 32)
    private ActivationTaskSourceType sourceType;

    @Column(name = "max_attempts", nullable = false)
    private Integer maxAttempts;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "last_attempt_id")
    private Long lastAttemptId;

    @Lob
    @Column(name = "payload_json", columnDefinition = "TEXT")
    private String payloadJson;

    @Lob
    @Column(name = "result_summary_json", columnDefinition = "TEXT")
    private String resultSummaryJson;

    @Column(name = "error_code", length = 64)
    private String errorCode;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

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
            this.status = ActivationTaskStatus.PENDING;
        }
        if (this.sourceType == null) {
            this.sourceType = ActivationTaskSourceType.ORDER_FULFILLMENT;
        }
        if (this.maxAttempts == null) {
            this.maxAttempts = 3;
        }
        if (this.attemptCount == null) {
            this.attemptCount = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public ActivationTask() {
    }

    public Long getId() {
        return id;
    }

    public String getTaskNo() {
        return taskNo;
    }

    public void setTaskNo(String taskNo) {
        this.taskNo = taskNo;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(Long orderItemId) {
        this.orderItemId = orderItemId;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public UUID getActivationToolId() {
        return activationToolId;
    }

    public void setActivationToolId(UUID activationToolId) {
        this.activationToolId = activationToolId;
    }

    public UUID getActivationToolVersionId() {
        return activationToolVersionId;
    }

    public void setActivationToolVersionId(UUID activationToolVersionId) {
        this.activationToolVersionId = activationToolVersionId;
    }

    public ActivationTaskStatus getStatus() {
        return status;
    }

    public void setStatus(ActivationTaskStatus status) {
        this.status = status;
    }

    public ActivationTaskSourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(ActivationTaskSourceType sourceType) {
        this.sourceType = sourceType;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Integer getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(Integer attemptCount) {
        this.attemptCount = attemptCount;
    }

    public Long getLastAttemptId() {
        return lastAttemptId;
    }

    public void setLastAttemptId(Long lastAttemptId) {
        this.lastAttemptId = lastAttemptId;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public String getResultSummaryJson() {
        return resultSummaryJson;
    }

    public void setResultSummaryJson(String resultSummaryJson) {
        this.resultSummaryJson = resultSummaryJson;
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

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}