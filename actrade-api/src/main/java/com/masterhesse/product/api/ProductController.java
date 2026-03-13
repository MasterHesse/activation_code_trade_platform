package com.masterhesse.product.api;

import com.masterhesse.product.api.request.CreateProductRequest;
import com.masterhesse.product.api.request.UpdateProductRequest;
import com.masterhesse.product.api.request.UpsertProductDetailRequest;
import com.masterhesse.product.application.ProductService;
import com.masterhesse.product.domain.Product;
import com.masterhesse.product.domain.ProductDetail;
import com.masterhesse.product.domain.ProductStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public Product create(@Valid @RequestBody CreateProductRequest request) {
        Product product = Product.builder()
                .merchantId(request.merchantId())
                .categoryId(request.categoryId())
                .name(request.name())
                .subtitle(request.subtitle())
                .description(request.description())
                .coverImage(request.coverImage())
                .deliveryMode(request.deliveryMode())
                .price(request.price())
                .originalPrice(request.originalPrice())
                .status(request.status() == null ? ProductStatus.DRAFT : request.status())
                .stockCount(request.stockCount() == null ? 0 : request.stockCount())
                .salesCount(request.salesCount() == null ? 0 : request.salesCount())
                .build();

        return productService.create(product);
    }

    @GetMapping
    public Page<Product> list(@RequestParam(required = false) UUID merchantId,
                              @RequestParam(required = false) UUID categoryId,
                              @RequestParam(required = false) ProductStatus status,
                              Pageable pageable) {
        return productService.list(merchantId, categoryId, status, pageable);
    }

    @GetMapping("/{productId}")
    public Product getById(@PathVariable UUID productId) {
        return productService.getById(productId);
    }

    @PutMapping("/{productId}")
    public Product update(@PathVariable UUID productId,
                          @Valid @RequestBody UpdateProductRequest request) {
        Product product = Product.builder()
                .categoryId(request.categoryId())
                .name(request.name())
                .subtitle(request.subtitle())
                .description(request.description())
                .coverImage(request.coverImage())
                .deliveryMode(request.deliveryMode())
                .price(request.price())
                .originalPrice(request.originalPrice())
                .status(request.status())
                .stockCount(request.stockCount() == null ? 0 : request.stockCount())
                .salesCount(request.salesCount() == null ? 0 : request.salesCount())
                .build();

        return productService.update(productId, product);
    }

    @DeleteMapping("/{productId}")
    public void delete(@PathVariable UUID productId) {
        productService.delete(productId);
    }

    @GetMapping("/{productId}/detail")
    public ProductDetail getDetail(@PathVariable UUID productId) {
        return productService.getDetail(productId);
    }

    @PutMapping("/{productId}/detail")
    public ProductDetail upsertDetail(@PathVariable UUID productId,
                                      @RequestBody UpsertProductDetailRequest request) {
        return productService.upsertDetail(productId, request);
    }
}