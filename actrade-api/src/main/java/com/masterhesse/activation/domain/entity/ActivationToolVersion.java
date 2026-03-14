// src/main/java/com/masterhesse/activation/domain/entity/ActivationToolVersion.java
package com.masterhesse.activation.domain.entity;

import com.masterhesse.activation.domain.enums.ActivationToolVersionStatus;
import com.masterhesse.activation.domain.enums.FileScanStatus;
import com.masterhesse.activation.domain.enums.RuntimeArch;
import com.masterhesse.activation.domain.enums.RuntimeOs;
import com.masterhesse.activation.domain.enums.RuntimeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name = "activation_tool_version",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tool_version_tool_id_version_name", columnNames = {"tool_id", "version_name"})
        },
        indexes = {
                @Index(name = "idx_tool_version_tool_id_status", columnList = "tool_id, status"),
                @Index(name = "idx_tool_version_file_id", columnList = "file_id")
        }
)
public class ActivationToolVersion {

    @Id
    @UuidGenerator
    @Column(name = "tool_version_id", nullable = false, updatable = false)
    private UUID toolVersionId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tool_id", nullable = false, foreignKey = @ForeignKey(name = "fk_activation_tool_version_tool"))
    private ActivationTool tool;

    @Column(name = "version_name", nullable = false, length = 64)
    private String versionName;

    @ManyToOne(optional = false)
    @JoinColumn(name = "file_id", nullable = false, foreignKey = @ForeignKey(name = "fk_activation_tool_version_file"))
    private FileAsset file;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "manifest_content", columnDefinition = "jsonb")
    private Map<String, Object> manifestContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "runtime_type", length = 32)
    private RuntimeType runtimeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "runtime_os", length = 32)
    private RuntimeOs runtimeOs;

    @Enumerated(EnumType.STRING)
    @Column(name = "runtime_arch", length = 32)
    private RuntimeArch runtimeArch;

    @Column(name = "entrypoint", length = 255)
    private String entrypoint;

    @Column(name = "exec_command", columnDefinition = "text")
    private String execCommand;

    @Column(name = "timeout_seconds")
    private Integer timeoutSeconds;

    @Column(name = "max_memory_mb")
    private Integer maxMemoryMb;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "scan_status", nullable = false, length = 32)
    private FileScanStatus scanStatus;

    @Column(name = "scan_report", columnDefinition = "text")
    private String scanReport;

    @Column(name = "review_remark", columnDefinition = "text")
    private String reviewRemark;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ActivationToolVersionStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected ActivationToolVersion() {
    }

    private ActivationToolVersion(
            ActivationTool tool,
            String versionName,
            FileAsset file,
            Map<String, Object> manifestContent,
            RuntimeType runtimeType,
            RuntimeOs runtimeOs,
            RuntimeArch runtimeArch,
            String entrypoint,
            String execCommand,
            Integer timeoutSeconds,
            Integer maxMemoryMb
    ) {
        this.tool = tool;
        this.versionName = versionName;
        this.file = file;
        this.manifestContent = manifestContent;
        this.runtimeType = runtimeType;
        this.runtimeOs = runtimeOs;
        this.runtimeArch = runtimeArch;
        this.entrypoint = entrypoint;
        this.execCommand = execCommand;
        this.timeoutSeconds = timeoutSeconds;
        this.maxMemoryMb = maxMemoryMb;
        this.fileSizeBytes = file.getFileSizeBytes();
        this.checksumSha256 = file.getChecksumSha256();
        this.scanStatus = file.getScanStatus();
        this.status = ActivationToolVersionStatus.DRAFT;
    }

    public static ActivationToolVersion create(
            ActivationTool tool,
            String versionName,
            FileAsset file,
            Map<String, Object> manifestContent,
            RuntimeType runtimeType,
            RuntimeOs runtimeOs,
            RuntimeArch runtimeArch,
            String entrypoint,
            String execCommand,
            Integer timeoutSeconds,
            Integer maxMemoryMb
    ) {
        return new ActivationToolVersion(
                tool,
                versionName,
                file,
                manifestContent,
                runtimeType,
                runtimeOs,
                runtimeArch,
                entrypoint,
                execCommand,
                timeoutSeconds,
                maxMemoryMb
        );
    }

    public void updateManifest(
            Map<String, Object> manifestContent,
            RuntimeType runtimeType,
            RuntimeOs runtimeOs,
            RuntimeArch runtimeArch,
            String entrypoint,
            String execCommand,
            Integer timeoutSeconds,
            Integer maxMemoryMb
    ) {
        this.manifestContent = manifestContent;
        this.runtimeType = runtimeType;
        this.runtimeOs = runtimeOs;
        this.runtimeArch = runtimeArch;
        this.entrypoint = entrypoint;
        this.execCommand = execCommand;
        this.timeoutSeconds = timeoutSeconds;
        this.maxMemoryMb = maxMemoryMb;
    }

    public void updateScanResult(FileScanStatus scanStatus, String scanReport) {
        this.scanStatus = scanStatus;
        this.scanReport = scanReport;
    }

    public void updateReviewRemark(String reviewRemark) {
        this.reviewRemark = reviewRemark;
    }

    public void changeStatus(ActivationToolVersionStatus status) {
        this.status = status;
    }

    public UUID getToolVersionId() {
        return toolVersionId;
    }

    public ActivationTool getTool() {
        return tool;
    }

    public UUID getToolId() {
        return tool.getToolId();
    }

    public String getVersionName() {
        return versionName;
    }

    public FileAsset getFile() {
        return file;
    }

    public UUID getFileId() {
        return file.getFileId();
    }

    public Map<String, Object> getManifestContent() {
        return manifestContent;
    }

    public RuntimeType getRuntimeType() {
        return runtimeType;
    }

    public RuntimeOs getRuntimeOs() {
        return runtimeOs;
    }

    public RuntimeArch getRuntimeArch() {
        return runtimeArch;
    }

    public String getEntrypoint() {
        return entrypoint;
    }

    public String getExecCommand() {
        return execCommand;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public Integer getMaxMemoryMb() {
        return maxMemoryMb;
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

    public String getReviewRemark() {
        return reviewRemark;
    }

    public ActivationToolVersionStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}