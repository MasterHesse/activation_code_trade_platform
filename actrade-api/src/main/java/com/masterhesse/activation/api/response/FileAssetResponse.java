// src/main/java/com/masterhesse/activation/api/response/FileAssetResponse.java
package com.masterhesse.activation.api.response;

import com.masterhesse.activation.domain.entity.FileAsset;
import com.masterhesse.activation.domain.enums.FileScanStatus;
import com.masterhesse.activation.domain.enums.StorageProvider;

import java.time.LocalDateTime;
import java.util.UUID;

public record FileAssetResponse(
        UUID fileId,
        StorageProvider storageProvider,
        String bucketName,
        String objectKey,
        String originalFilename,
        String storedFilename,
        String contentType,
        Long fileSizeBytes,
        String checksumSha256,
        FileScanStatus scanStatus,
        String scanReport,
        UUID uploadedBy,
        LocalDateTime createdAt
) {
    public static FileAssetResponse from(FileAsset entity) {
        return new FileAssetResponse(
                entity.getFileId(),
                entity.getStorageProvider(),
                entity.getBucketName(),
                entity.getObjectKey(),
                entity.getOriginalFilename(),
                entity.getStoredFilename(),
                entity.getContentType(),
                entity.getFileSizeBytes(),
                entity.getChecksumSha256(),
                entity.getScanStatus(),
                entity.getScanReport(),
                entity.getUploadedBy(),
                entity.getCreatedAt()
        );
    }
}