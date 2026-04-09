package com.actrade.activationrunner.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ToolVersionDto(
        String versionName,
        String runtimeType,
        String runtimeOs,
        String runtimeArch,
        String entrypoint,
        String execCommand,
        Integer timeoutSeconds,
        Integer maxMemoryMb,
        List<Integer> successExitCodes,
        Map<String, Object> manifestContent
) {
}