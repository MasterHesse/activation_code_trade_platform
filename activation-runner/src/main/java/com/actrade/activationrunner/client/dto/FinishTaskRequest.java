package com.actrade.activationrunner.client.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record FinishTaskRequest(
        Boolean success,
        String summary,

        Integer exitCode,
        String errorCode,
        String errorMessage,

        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        Long durationMs,

        String runnerInstanceId,
        Boolean timedOut,

        List<ArtifactItemDto> artifacts,
        Map<String, String> diagnostics
) {
}