package com.masterhesse.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "product",
        indexes = {
                @Index(name = "idx_product_merchant_id", columnList = "merchant_id"),
                @Index(name = "idx_product_category_id", columnList = "category_id"),
                @Index(name = "idx_product_status", columnList = "status"),
                @Index(name = "idx_product_delivery_mode", columnList = "delivery_mode"),
                @Index(name = "idx_product_created_at", columnList = "created_at")
        }
)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "subtitle", length = 255)
    private String subtitle;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_image", length = 255)
    private String coverImage;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_mode", nullable = false, length = 32)
    private DeliveryMode deliveryMode;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "original_price", precision = 12, scale = 2)
    private BigDecimal originalPrice;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ProductStatus status = ProductStatus.DRAFT;

    @Builder.Default
    @Column(name = "stock_count", nullable = false)
    private Integer stockCount = 0;

    @Builder.Default
    @Column(name = "sales_count", nullable = false)
    private Integer salesCount = 0;

    @Version
    @Builder.Default
    @Column(name = "version_no", nullable = false)
    private Integer versionNo = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}