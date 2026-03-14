// src/main/java/com/masterhesse/activation/api/request/UpdateActivationToolVersionStatusRequest.java
package com.masterhesse.activation.api.request;

import com.masterhesse.activation.domain.enums.ActivationToolVersionStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateActivationToolVersionStatusRequest(
        @NotNull ActivationToolVersionStatus status
) {
}