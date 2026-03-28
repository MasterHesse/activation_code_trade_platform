package com.masterhesse.activation.api.request;

import com.masterhesse.activation.domain.enums.ActivationTaskSourceType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public class CreateActivationTaskRequest {

    private Long orderId;

    private Long orderItemId;

    private Long merchantId;

    @NotNull(message = "activationToolId cannot be null")
    private UUID activationToolId;

    @NotNull(message = "activationToolVersionId cannot be null")
    private UUID activationToolVersionId;

    private ActivationTaskSourceType sourceType;

    @Min(value = 1, message = "maxAttempts must be >= 1")
    private Integer maxAttempts;

    /**
     * 任务输入参数，先直接接收 JSON 字符串。
     * 后续你也可以改成对象，再由 ObjectMapper 序列化。
     */
    @NotBlank(message = "payloadJson cannot be blank")
    private String payloadJson;

    private LocalDateTime scheduledAt;

    public CreateActivationTaskRequest() {
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

    public void setActivationToolVersionId(@NotNull(message = "activationToolVersionId cannot be null") UUID activationToolVersionId) {
        this.activationToolVersionId = activationToolVersionId;
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

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }
}