// src/main/java/com/masterhesse/activation/api/request/UpdateActivationCodeStatusRequest.java
package com.masterhesse.activation.api.request;

import com.masterhesse.activation.domain.enums.ActivationCodeStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateActivationCodeStatusRequest(
        @NotNull ActivationCodeStatus status
) {
}