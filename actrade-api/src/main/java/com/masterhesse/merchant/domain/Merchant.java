package com.masterhesse.merchant.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "merchant",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_merchant_user_id", columnNames = "user_id")
        },
        indexes = {
                @Index(name = "idx_merchant_audit_status", columnList = "audit_status"),
                @Index(name = "idx_merchant_created_at", columnList = "created_at")
        }
)
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "merchant_id", nullable = false, updatable = false)
    private UUID merchantId;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "merchant_name", nullable = false, length = 128)
    private String merchantName;

    @Enumerated(EnumType.STRING)
    @Column(name = "merchant_type", nullable = false, length = 32)
    private MerchantType merchantType;

    @Column(name = "contact_name", length = 64)
    private String contactName;

    @Column(name = "contact_email", length = 128)
    private String contactEmail;

    @Column(name = "contact_phone", length = 32)
    private String contactPhone;

    @Column(name = "license_info", columnDefinition = "TEXT")
    private String licenseInfo;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "audit_status",nullable = false, length = 32)
    private MerchantAuditStatus auditStatus = MerchantAuditStatus.PENDING;

    @Column(name = "audit_remark", columnDefinition = "TEXT")
    private String auditRemark;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}