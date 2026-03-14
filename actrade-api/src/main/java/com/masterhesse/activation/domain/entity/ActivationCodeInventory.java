// src/main/java/com/masterhesse/activation/domain/entity/ActivationCodeInventory.java
package com.masterhesse.activation.domain.entity;

import com.masterhesse.activation.domain.enums.ActivationCodeStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "activation_code_inventory",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_code_inventory_product_code_hash", columnNames = {"product_id", "code_value_hash"})
        },
        indexes = {
                @Index(name = "idx_code_inventory_product_status", columnList = "product_id, status"),
                @Index(name = "idx_code_inventory_batch_no", columnList = "batch_no"),
                @Index(name = "idx_code_inventory_assigned_order_id", columnList = "assigned_order_id"),
                @Index(name = "idx_code_inventory_expired_at", columnList = "expired_at")
        }
)
public class ActivationCodeInventory {

    @Id
    @UuidGenerator
    @Column(name = "code_id", nullable = false, updatable = false)
    private UUID codeId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "batch_no", nullable = false, length = 64)
    private String batchNo;

    @Column(name = "code_value_encrypted", nullable = false, columnDefinition = "text")
    private String codeValueEncrypted;

    @Column(name = "code_value_masked", nullable = false, length = 128)
    private String codeValueMasked;

    @Column(name = "code_value_hash", nullable = false, length = 128)
    private String codeValueHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ActivationCodeStatus status;

    @Column(name = "assigned_order_id")
    private UUID assignedOrderId;

    @Column(name = "assigned_order_item_id")
    private UUID assignedOrderItemId;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "remark", length = 255)
    private String remark;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected ActivationCodeInventory() {
    }

    private ActivationCodeInventory(
            UUID productId,
            UUID merchantId,
            String batchNo,
            String codeValueEncrypted,
            String codeValueMasked,
            String codeValueHash,
            LocalDateTime expiredAt,
            String remark
    ) {
        this.productId = productId;
        this.merchantId = merchantId;
        this.batchNo = batchNo;
        this.codeValueEncrypted = codeValueEncrypted;
        this.codeValueMasked = codeValueMasked;
        this.codeValueHash = codeValueHash;
        this.expiredAt = expiredAt;
        this.remark = remark;
        this.status = ActivationCodeStatus.AVAILABLE;
    }

    public static ActivationCodeInventory create(
            UUID productId,
            UUID merchantId,
            String batchNo,
            String codeValueEncrypted,
            String codeValueMasked,
            String codeValueHash,
            LocalDateTime expiredAt,
            String remark
    ) {
        return new ActivationCodeInventory(
                productId,
                merchantId,
                batchNo,
                codeValueEncrypted,
                codeValueMasked,
                codeValueHash,
                expiredAt,
                remark
        );
    }

    public void changeStatus(ActivationCodeStatus status) {
        this.status = status;
    }

    public void assignOrder(UUID assignedOrderId, UUID assignedOrderItemId) {
        this.assignedOrderId = assignedOrderId;
        this.assignedOrderItemId = assignedOrderItemId;
    }

    public void clearAssignment() {
        this.assignedOrderId = null;
        this.assignedOrderItemId = null;
    }

    public void voidCode() {
        this.status = ActivationCodeStatus.VOID;
    }

    public UUID getCodeId() {
        return codeId;
    }

    public UUID getProductId() {
        return productId;
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public String getCodeValueEncrypted() {
        return codeValueEncrypted;
    }

    public String getCodeValueMasked() {
        return codeValueMasked;
    }

    public String getCodeValueHash() {
        return codeValueHash;
    }

    public ActivationCodeStatus getStatus() {
        return status;
    }

    public UUID getAssignedOrderId() {
        return assignedOrderId;
    }

    public UUID getAssignedOrderItemId() {
        return assignedOrderItemId;
    }

    public LocalDateTime getExpiredAt() {
        return expiredAt;
    }

    public String getRemark() {
        return remark;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}