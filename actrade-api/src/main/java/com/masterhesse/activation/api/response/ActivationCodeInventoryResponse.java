// src/main/java/com/masterhesse/activation/api/response/ActivationCodeInventoryResponse.java
package com.masterhesse.activation.api.response;

import com.masterhesse.activation.domain.entity.ActivationCodeInventory;
import com.masterhesse.activation.domain.enums.ActivationCodeStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ActivationCodeInventoryResponse(
        UUID codeId,
        UUID productId,
        UUID merchantId,
        String batchNo,
        String codeValueEncrypted,
        String codeValueMasked,
        String codeValueHash,
        ActivationCodeStatus status,
        UUID assignedOrderId,
        UUID assignedOrderItemId,
        LocalDateTime expiredAt,
        String remark,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ActivationCodeInventoryResponse from(ActivationCodeInventory entity) {
        return new ActivationCodeInventoryResponse(
                entity.getCodeId(),
                entity.getProductId(),
                entity.getMerchantId(),
                entity.getBatchNo(),
                entity.getCodeValueEncrypted(),
                entity.getCodeValueMasked(),
                entity.getCodeValueHash(),
                entity.getStatus(),
                entity.getAssignedOrderId(),
                entity.getAssignedOrderItemId(),
                entity.getExpiredAt(),
                entity.getRemark(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}