// src/main/java/com/masterhesse/activation/domain/entity/FileAsset.java
package com.masterhesse.activation.domain.entity;

import com.masterhesse.activation.domain.enums.FileScanStatus;
import com.masterhesse.activation.domain.enums.StorageProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "file_asset",
        indexes = {
                @Index(name = "idx_file_asset_checksum_sha256", columnList = "checksum_sha256"),
                @Index(name = "idx_file_asset_bucket_object_key", columnList = "bucket_name, object_key"),
                @Index(name = "idx_file_asset_uploaded_by", columnList = "uploaded_by")
        }
)
public class FileAsset {

    @Id
    @UuidGenerator
    @Column(name = "file_id", nullable = false, updatable = false)
    private UUID fileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_provider", nullable = false, length = 32)
    private StorageProvider storageProvider;

    @Column(name = "bucket_name", nullable = false, length = 128)
    private String bucketName;

    @Column(name = "object_key", nullable = false, length = 255)
    private String objectKey;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false, length = 255)
    private String storedFilename;

    @Column(name = "content_type", nullable = false, length = 128)
    private String contentType;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Column(name = "checksum_sha256", nullable = false, length = 64)
    private String checksumSha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "scan_status", nullable = false, length = 32)
    private FileScanStatus scanStatus;

    @Column(name = "scan_report", columnDefinition = "text")
    private String scanReport;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected FileAsset() {
    }

    private FileAsset(
            StorageProvider storageProvider,
            String bucketName,
            String objectKey,
            String originalFilename,
            String storedFilename,
            String contentType,
            Long fileSizeBytes,
            String checksumSha256,
            UUID uploadedBy
    ) {
        this.storageProvider = storageProvider;
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.contentType = contentType;
        this.fileSizeBytes = fileSizeBytes;
        this.checksumSha256 = checksumSha256;
        this.uploadedBy = uploadedBy;
        this.scanStatus = FileScanStatus.PENDING;
    }

    public static FileAsset create(
            StorageProvider storageProvider,
            String bucketName,
            String objectKey,
            String originalFilename,
            String storedFilename,
            String contentType,
            Long fileSizeBytes,
            String checksumSha256,
            UUID uploadedBy
    ) {
        return new FileAsset(
                storageProvider,
                bucketName,
                objectKey,
                originalFilename,
                storedFilename,
                contentType,
                fileSizeBytes,
                checksumSha256,
                uploadedBy
        );
    }

    public void updateScanResult(FileScanStatus scanStatus, String scanReport) {
        this.scanStatus = scanStatus;
        this.scanReport = scanReport;
    }

    public UUID getFileId() {
        return fileId;
    }

    public StorageProvider getStorageProvider() {
        return storageProvider;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getStoredFilename() {
        return storedFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public FileScanStatus getScanStatus() {
        return scanStatus;
    }

    public String getScanReport() {
        return scanReport;
    }

    public UUID getUploadedBy() {
        return uploadedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}