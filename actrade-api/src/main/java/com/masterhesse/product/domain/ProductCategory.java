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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "product_category",
        indexes = {
                @Index(name = "idx_product_category_parent_id", columnList = "parent_id"),
                @Index(name = "idx_product_category_status", columnList = "status"),
                @Index(name = "idx_product_category_sort_no", columnList = "sort_no")
        }
)
public class ProductCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "category_id", nullable = false, updatable = false)
    private UUID categoryId;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Builder.Default
    @Column(name = "sort_no", nullable = false)
    private Integer sortNo = 0;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ProductCategoryStatus status = ProductCategoryStatus.ENABLED;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}