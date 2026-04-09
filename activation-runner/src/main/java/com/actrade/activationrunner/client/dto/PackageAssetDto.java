package com.actrade.activationrunner.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PackageAssetDto(
        String fileId,
        String bucketName,
        String objectKey,
        String originalFilename,
        String storedFilename,
        String checksumSha256,
        Long fileSizeBytes
) {
}