// src/main/java/com/masterhesse/activation/api/request/CreateActivationToolVersionRequest.java
package com.masterhesse.activation.api.request;

import com.masterhesse.activation.domain.enums.RuntimeArch;
import com.masterhesse.activation.domain.enums.RuntimeOs;
import com.masterhesse.activation.domain.enums.RuntimeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

public record CreateActivationToolVersionRequest(
        @NotNull UUID toolId,
        @NotBlank @Size(max = 64) String versionName,
        @NotNull UUID fileId,
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