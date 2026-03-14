// src/main/java/com/masterhesse/activation/api/response/ActivationToolVersionResponse.java
package com.masterhesse.activation.api.response;

import com.masterhesse.activation.domain.entity.ActivationToolVersion;
import com.masterhesse.activation.domain.enums.ActivationToolVersionStatus;
import com.masterhesse.activation.domain.enums.FileScanStatus;
import com.masterhesse.activation.domain.enums.RuntimeArch;
import com.masterhesse.activation.domain.enums.RuntimeOs;
import com.masterhesse.activation.domain.enums.RuntimeType;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record ActivationToolVersionResponse(
        UUID toolVersionId,
        UUID toolId,
        String versionName,
        UUID fileId,
        Map<String, Object> manifestContent,
        RuntimeType runtimeType,
        RuntimeOs runtimeOs,
        RuntimeArch runtimeArch,
        String entrypoint,
        String execCommand,
        Integer timeoutSeconds,
        Integer maxMemoryMb,
        Long fileSizeBytes,
        String checksumSha256,
        FileScanStatus scanStatus,
        String scanReport,
        String reviewRemark,
        ActivationToolVersionStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ActivationToolVersionResponse from(ActivationToolVersion entity) {
        return new ActivationToolVersionResponse(
                entity.getToolVersionId(),
                entity.getToolId(),
                entity.getVersionName(),
                entity.getFileId(),
                entity.getManifestContent(),
                entity.getRuntimeType(),
                entity.getRuntimeOs(),
                entity.getRuntimeArch(),
                entity.getEntrypoint(),
                entity.getExecCommand(),
                entity.getTimeoutSeconds(),
                entity.getMaxMemoryMb(),
                entity.getFileSizeBytes(),
                entity.getChecksumSha256(),
                entity.getScanStatus(),
                entity.getScanReport(),
                entity.getReviewRemark(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}