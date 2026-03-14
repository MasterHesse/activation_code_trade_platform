// src/main/java/com/masterhesse/activation/domain/entity/ActivationTool.java
package com.masterhesse.activation.domain.entity;

import com.masterhesse.activation.domain.enums.ActivationToolAuditStatus;
import com.masterhesse.activation.domain.enums.ActivationToolStatus;
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
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "activation_tool",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_activation_tool_product_id", columnNames = "product_id")
        },
        indexes = {
                @Index(name = "idx_activation_tool_merchant_status", columnList = "merchant_id, tool_status"),
                @Index(name = "idx_activation_tool_current_version_id", columnList = "current_version_id")
        }
)
public class ActivationTool {

    @Id
    @UuidGenerator
    @Column(name = "tool_id", nullable = false, updatable = false)
    private UUID toolId;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "tool_name", nullable = false, length = 128)
    private String toolName;

    @ManyToOne
    @JoinColumn(
            name = "current_version_id",
            foreignKey = @ForeignKey(name = "fk_activation_tool_current_version")
    )
    private ActivationToolVersion currentVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tool_status", nullable = false, length = 32)
    private ActivationToolStatus toolStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "audit_status", nullable = false, length = 32)
    private ActivationToolAuditStatus auditStatus;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected ActivationTool() {
    }

    private ActivationTool(UUID merchantId, UUID productId, String toolName) {
        this.merchantId = merchantId;
        this.productId = productId;
        this.toolName = toolName;
        this.toolStatus = ActivationToolStatus.DISABLED;
        this.auditStatus = ActivationToolAuditStatus.PENDING;
    }

    public static ActivationTool create(UUID merchantId, UUID productId, String toolName) {
        return new ActivationTool(merchantId, productId, toolName);
    }

    public void rename(String toolName) {
        this.toolName = toolName;
    }

    public void changeToolStatus(ActivationToolStatus toolStatus) {
        this.toolStatus = toolStatus;
    }

    public void changeAuditStatus(ActivationToolAuditStatus auditStatus) {
        this.auditStatus = auditStatus;
    }

    public void changeCurrentVersion(ActivationToolVersion currentVersion) {
        this.currentVersion = currentVersion;
    }

    public UUID getToolId() {
        return toolId;
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getToolName() {
        return toolName;
    }

    public ActivationToolVersion getCurrentVersion() {
        return currentVersion;
    }

    public UUID getCurrentVersionId() {
        return currentVersion != null ? currentVersion.getToolVersionId() : null;
    }

    public ActivationToolStatus getToolStatus() {
        return toolStatus;
    }

    public ActivationToolAuditStatus getAuditStatus() {
        return auditStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}