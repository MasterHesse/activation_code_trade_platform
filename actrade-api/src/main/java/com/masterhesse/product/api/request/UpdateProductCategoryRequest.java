package com.masterhesse.product.api.request;

import com.masterhesse.product.domain.ProductCategoryStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateProductCategoryRequest(
        UUID parentId,

        @NotBlank
        @Size(max = 64)
        String name,

        Integer sortNo,

        ProductCategoryStatus status
) {
}