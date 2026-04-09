package com.masterhesse.activation.api.internal.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record FinishTaskRequest(
        @NotNull Boolean success,
        String summary,
        Integer exitCode,
        String errorCode,
        String errorMessage,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        @PositiveOrZero Long durationMs,
        @NotBlank String runnerInstanceId,
        @NotNull Boolean timedOut,
        List<@Valid FinishTaskArtifactItemRequest> artifacts,
        Map<String, String> diagnostics
) {
}