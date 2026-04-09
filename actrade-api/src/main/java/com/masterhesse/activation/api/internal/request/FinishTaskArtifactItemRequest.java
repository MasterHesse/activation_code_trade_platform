package com.masterhesse.activation.api.internal.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record FinishTaskArtifactItemRequest(
        @NotBlank String artifactType,
        UUID fileId,
        String bucketName,
        String objectKey,
        String originalFilename,
        String storedFilename,
        @Positive Long fileSizeBytes,
        String checksumSha256
) {
}