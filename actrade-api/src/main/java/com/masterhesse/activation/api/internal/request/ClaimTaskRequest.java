package com.masterhesse.activation.api.internal.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ClaimTaskRequest(
        @NotBlank String taskNo,
        @NotBlank String runnerInstanceId,
        @Positive Integer expectedAttemptNo,
        @NotBlank String messageId
) {
}