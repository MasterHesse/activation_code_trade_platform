// src/main/java/com/masterhesse/activation/api/request/UpdateActivationToolAuditStatusRequest.java
package com.masterhesse.activation.api.request;

import com.masterhesse.activation.domain.enums.ActivationToolAuditStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateActivationToolAuditStatusRequest(
        @NotNull ActivationToolAuditStatus auditStatus
) {
}