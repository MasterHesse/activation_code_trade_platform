package com.actrade.activationrunner.application;

import java.nio.file.Path;

/**
 * 工具包下载结果
 */
public record PackageDownloadResult(
        boolean success,
        Path downloadedFile,
        Path extractedDir,
        String checksum,
        Long fileSizeBytes,
        String errorCode,
        String errorMessage
) {

    public static PackageDownloadResult success(
            Path downloadedFile,
            Path extractedDir,
            String checksum,
            Long fileSizeBytes
    ) {
        return new PackageDownloadResult(
                true,
                downloadedFile,
                extractedDir,
                checksum,
                fileSizeBytes,
                null,
                null
        );
    }

    public static PackageDownloadResult failure(String errorCode, String errorMessage) {
        return new PackageDownloadResult(
                false,
                null,
                null,
                null,
                null,
                errorCode,
                errorMessage
        );
    }

    public static PackageDownloadResult failure(String errorMessage) {
        return failure("DOWNLOAD_FAILED", errorMessage);
    }
}
