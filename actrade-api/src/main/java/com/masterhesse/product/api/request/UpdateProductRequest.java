package com.masterhesse.product.api.request;

import com.masterhesse.product.domain.DeliveryMode;
import com.masterhesse.product.domain.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 商品更新请求 - 支持部分字段更新
 */
public record UpdateProductRequest(
        UUID categoryId,

        @Size(max = 128)
        String name,

        @Size(max = 255)
        String subtitle,

        String description,

        @Size(max = 255)
        String coverImage,

        DeliveryMode deliveryMode,

        @DecimalMin(value = "0.00")
        BigDecimal price,

        @DecimalMin(value = "0.00")
        BigDecimal originalPrice,

        ProductStatus status,

        Integer stockCount,

        Integer salesCount
) {
}