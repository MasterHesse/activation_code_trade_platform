package com.actrade.activationrunner.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ClaimTaskResponse(
        boolean claimed,
        String reason,

        Long taskId,
        String taskNo,
        Long attemptId,
        Integer attemptNo,

        Long merchantId,
        String activationToolVersionId,
        String payloadJson,
        OffsetDateTime scheduledAt,

        ToolVersionDto toolVersion,
        PackageAssetDto packageAsset,

        Map<String, String> runnerEnv
) {
}