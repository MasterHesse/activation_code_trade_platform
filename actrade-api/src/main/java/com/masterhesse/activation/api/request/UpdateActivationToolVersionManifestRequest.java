// src/main/java/com/masterhesse/activation/api/request/UpdateActivationToolVersionManifestRequest.java
package com.masterhesse.activation.api.request;

import com.masterhesse.activation.domain.enums.RuntimeArch;
import com.masterhesse.activation.domain.enums.RuntimeOs;
import com.masterhesse.activation.domain.enums.RuntimeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record UpdateActivationToolVersionManifestRequest(
        @NotNull Map<String, Object> manifestContent,
        @NotNull RuntimeType runtimeType,
        @NotNull RuntimeOs runtimeOs,
        @NotNull RuntimeArch runtimeArch,
        @NotBlank @Size(max = 255) String entrypoint,
        @NotBlank String execCommand,
        @NotNull @Positive Integer timeoutSeconds,
        @NotNull @Positive Integer maxMemoryMb
) {
}