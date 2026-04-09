package com.actrade.activationrunner.client.dto;

public record ClaimTaskRequest(
        String taskNo,
        String runnerInstanceId,
        Integer expectedAttemptNo,
        String messageId
) {
}