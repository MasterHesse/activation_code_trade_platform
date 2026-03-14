// src/main/java/com/masterhesse/activation/api/request/UpdateActivationToolStatusRequest.java
package com.masterhesse.activation.api.request;

import com.masterhesse.activation.domain.enums.ActivationToolStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateActivationToolStatusRequest(
        @NotNull ActivationToolStatus toolStatus
) {
}