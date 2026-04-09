package com.actrade.activationrunner.application;

import com.actrade.activationrunner.client.dto.ArtifactItemDto;

import java.util.List;

/**
 * 产物上传结果 DTO
 */
public record ArtifactUploadResult(
        boolean success,
        List<ArtifactItemDto> artifacts,
        int uploadedCount,
        long totalSizeBytes,
        String errorCode,
        String errorMessage
) {

    public static ArtifactUploadResult success(List<ArtifactItemDto> artifacts, int count, long size) {
        return new ArtifactUploadResult(true, artifacts, count, size, null, null);
    }

    public static ArtifactUploadResult failure(String code, String message) {
        return new ArtifactUploadResult(false, List.of(), 0, 0, code, message);
    }

    public static ArtifactUploadResult empty() {
        return new ArtifactUploadResult(true, List.of(), 0, 0, null, null);
    }
}
