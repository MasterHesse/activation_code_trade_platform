// src/main/java/com/masterhesse/activation/api/response/ActivationToolResponse.java
package com.masterhesse.activation.api.response;

import com.masterhesse.activation.domain.entity.ActivationTool;
import com.masterhesse.activation.domain.enums.ActivationToolAuditStatus;
import com.masterhesse.activation.domain.enums.ActivationToolStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ActivationToolResponse(
        UUID toolId,
        UUID merchantId,
        UUID productId,
        String toolName,
        UUID currentVersionId,
        ActivationToolStatus toolStatus,
        ActivationToolAuditStatus auditStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ActivationToolResponse from(ActivationTool entity) {
        return new ActivationToolResponse(
                entity.getToolId(),
                entity.getMerchantId(),
                entity.getProductId(),
                entity.getToolName(),
                entity.getCurrentVersionId(),
                entity.getToolStatus(),
                entity.getAuditStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}