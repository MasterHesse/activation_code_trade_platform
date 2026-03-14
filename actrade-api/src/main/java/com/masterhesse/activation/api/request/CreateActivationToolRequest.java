// src/main/java/com/masterhesse/activation/api/request/CreateActivationToolRequest.java
package com.masterhesse.activation.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateActivationToolRequest(
        @NotNull UUID merchantId,
        @NotNull UUID productId,
        @NotBlank @Size(max = 128) String toolName
) {
}