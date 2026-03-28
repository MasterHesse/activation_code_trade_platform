package com.masterhesse.activation.api.response;

import com.masterhesse.activation.domain.entity.ActivationTask;
import com.masterhesse.activation.domain.enums.ActivationTaskSourceType;
import com.masterhesse.activation.domain.enums.ActivationTaskStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class ActivationTaskResponse {

    private Long id;
    private String taskNo;
    private Long orderId;
    private Long orderItemId;
    private Long merchantId;
    private UUID activationToolId;
    private UUID activationToolVersionId;
    private ActivationTaskStatus status;
    private ActivationTaskSourceType sourceType;
    private Integer maxAttempts;
    private Integer attemptCount;
    private Long lastAttemptId;
    private String payloadJson;
    private String resultSummaryJson;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime scheduledAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ActivationTaskResponse() {
    }

    public static ActivationTaskResponse from(ActivationTask task) {
        ActivationTaskResponse response = new ActivationTaskResponse();
        response.setId(task.getId());
        response.setTaskNo(task.getTaskNo());
        response.setOrderId(task.getOrderId());
        response.setOrderItemId(task.getOrderItemId());
        response.setMerchantId(task.getMerchantId());
        response.setActivationToolId(task.getActivationToolId());
        response.setActivationToolVersionId(task.getActivationToolVersionId());
        response.setStatus(task.getStatus());
        response.setSourceType(task.getSourceType());
        response.setMaxAttempts(task.getMaxAttempts());
        response.setAttemptCount(task.getAttemptCount());
        response.setLastAttemptId(task.getLastAttemptId());
        response.setPayloadJson(task.getPayloadJson());
        response.setResultSummaryJson(task.getResultSummaryJson());
        response.setErrorCode(task.getErrorCode());
        response.setErrorMessage(task.getErrorMessage());
        response.setScheduledAt(task.getScheduledAt());
        response.setStartedAt(task.getStartedAt());
        response.setFinishedAt(task.getFinishedAt());
        response.setCreatedAt(task.getCreatedAt());
        response.setUpdatedAt(task.getUpdatedAt());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}