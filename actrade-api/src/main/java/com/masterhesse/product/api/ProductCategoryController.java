package com.masterhesse.product.api;

import com.masterhesse.product.api.request.CreateProductCategoryRequest;
import com.masterhesse.product.api.request.UpdateProductCategoryRequest;
import com.masterhesse.product.application.ProductCategoryService;
import com.masterhesse.product.domain.ProductCategory;
import com.masterhesse.product.domain.ProductCategoryStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/product-categories")
@RequiredArgsConstructor
public class ProductCategoryController {

    private final ProductCategoryService productCategoryService;

    @PostMapping
    public ProductCategory create(@Valid @RequestBody CreateProductCategoryRequest request) {
        ProductCategory category = ProductCategory.builder()
                .parentId(request.parentId())
                .name(request.name())
                .sortNo(request.sortNo() == null ? 0 : request.sortNo())
                .status(request.status() == null ? ProductCategoryStatus.ENABLED : request.status())
                .build();

        return productCategoryService.create(category);
    }

    @GetMapping
    public List<ProductCategory> list(@RequestParam(required = false) ProductCategoryStatus status) {
        if (status != null) {
            return productCategoryService.listByStatus(status);
        }
        return productCategoryService.listAll();
    }

    @GetMapping("/{categoryId}")
    public ProductCategory getById(@PathVariable UUID categoryId) {
        return productCategoryService.getById(categoryId);
    }

    @PutMapping("/{categoryId}")
    public ProductCategory update(@PathVariable UUID categoryId,
                                  @Valid @RequestBody UpdateProductCategoryRequest request) {
        ProductCategory category = ProductCategory.builder()
                .parentId(request.parentId())
                .name(request.name())
                .sortNo(request.sortNo())
                .status(request.status())
                .build();

        return productCategoryService.update(categoryId, category);
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> delete(@PathVariable UUID categoryId) {
        productCategoryService.delete(categoryId);
        return ResponseEntity.noContent().build();
    }
}