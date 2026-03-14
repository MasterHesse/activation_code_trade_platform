// src/main/java/com/masterhesse/activation/api/request/UpdateActivationToolNameRequest.java
package com.masterhesse.activation.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateActivationToolNameRequest(
        @NotBlank @Size(max = 128) String toolName
) {
}