package com.actrade.activationrunner.client.dto;

public record ArtifactItemDto(
        String artifactType,
        String fileName,
        String storageUri,
        String bucketName,
        String objectKey,
        String mediaType,
        Long fileSize,
        String checksumSha256
) {
}