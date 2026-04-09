package com.actrade.activationrunner.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FinishTaskResponse(
        boolean accepted,
        boolean duplicate,
        String taskStatus,
        boolean retryScheduled,
        Integer nextAttemptNo,
        OffsetDateTime nextScheduledAt
) {
}