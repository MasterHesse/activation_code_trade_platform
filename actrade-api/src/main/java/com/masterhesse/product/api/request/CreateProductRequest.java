package com.masterhesse.product.api.request;

import com.masterhesse.product.domain.DeliveryMode;
import com.masterhesse.product.domain.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateProductRequest(
        @NotNull
        UUID merchantId,

        @NotNull
        UUID categoryId,

        @NotBlank
        @Size(max = 128)
        String name,

        @Size(max = 255)
        String subtitle,

        String description,

        @Size(max = 255)
        String coverImage,

        @NotNull
        DeliveryMode deliveryMode,

        @NotNull
        @DecimalMin(value = "0.00")
        BigDecimal price,

        @DecimalMin(value = "0.00")
        BigDecimal originalPrice,

        ProductStatus status,

        Integer stockCount,

        Integer salesCount
) {
}