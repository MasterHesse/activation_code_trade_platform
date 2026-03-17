package com.masterhesse.product.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "product_detail")
public class ProductDetail {

    @Id
    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @JsonIgnore
    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "product_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_product_detail_product")
    )
    private Product product;

    @Column(name = "detail_markdown", columnDefinition = "TEXT")
    private String detailMarkdown;

    @Column(name = "usage_guide", columnDefinition = "TEXT")
    private String usageGuide;

    @Column(name = "activation_notice", columnDefinition = "TEXT")
    private String activationNotice;

    @Column(name = "refund_policy", columnDefinition = "TEXT")
    private String refundPolicy;

    @Column(name = "faq_content", columnDefinition = "TEXT")
    private String faqContent;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}