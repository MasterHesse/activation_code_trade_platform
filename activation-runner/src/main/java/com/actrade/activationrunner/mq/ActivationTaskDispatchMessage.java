package com.actrade.activationrunner.mq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ActivationTaskDispatchMessage(
        Long taskId,
        String taskNo,
        Integer expectedAttemptNo,
        String messageId,
        String traceId
) {
}