// src/main/java/com/masterhesse/activation/api/request/CreateFileAssetRequest.java
package com.masterhesse.activation.api.request;

import com.masterhesse.activation.domain.enums.StorageProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateFileAssetRequest(
        @NotNull StorageProvider storageProvider,
        @NotBlank @Size(max = 128) String bucketName,
        @NotBlank @Size(max = 255) String objectKey,
        @NotBlank @Size(max = 255) String originalFilename,
        @NotBlank @Size(max = 255) String storedFilename,
        @NotBlank @Size(max = 128) String contentType,
        @NotNull @Positive Long fileSizeBytes,
        @NotBlank @Size(max = 64) String checksumSha256,
        @NotNull UUID uploadedBy
) {
}