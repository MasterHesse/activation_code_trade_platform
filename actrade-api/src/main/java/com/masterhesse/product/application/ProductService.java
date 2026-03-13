package com.masterhesse.product.application;

import com.masterhesse.merchant.persistence.MerchantRepository;
import com.masterhesse.product.api.request.UpsertProductDetailRequest;
import com.masterhesse.product.domain.Product;
import com.masterhesse.product.domain.ProductDetail;
import com.masterhesse.product.domain.ProductStatus;
import com.masterhesse.product.persistence.ProductCategoryRepository;
import com.masterhesse.product.persistence.ProductDetailRepository;
import com.masterhesse.product.persistence.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductDetailRepository productDetailRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final MerchantRepository merchantRepository;

    @Transactional
    public Product create(Product product) {
        validateCreateOrUpdate(product.getMerchantId(), product.getCategoryId());

        if (product.getStockCount() == null) {
            product.setStockCount(0);
        }
        if (product.getSalesCount() == null) {
            product.setSalesCount(0);
        }
        if (product.getVersionNo() == null) {
            product.setVersionNo(0);
        }

        return productRepository.save(product);
    }

    public Product getById(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("商品不存在: " + productId));
    }

    public Page<Product> list(UUID merchantId, UUID categoryId, ProductStatus status, Pageable pageable) {
        if (merchantId != null) {
            return productRepository.findByMerchantId(merchantId, pageable);
        }
        if (categoryId != null) {
            return productRepository.findByCategoryId(categoryId, pageable);
        }
        if (status != null) {
            return productRepository.findByStatus(status, pageable);
        }
        return productRepository.findAll(pageable);
    }

    @Transactional
    public Product update(UUID productId, Product incoming) {
        Product existing = getById(productId);

        if (!productCategoryRepository.existsById(incoming.getCategoryId())) {
            throw new IllegalArgumentException("商品分类不存在: " + incoming.getCategoryId());
        }

        existing.setCategoryId(incoming.getCategoryId());
        existing.setName(incoming.getName());
        existing.setSubtitle(incoming.getSubtitle());
        existing.setDescription(incoming.getDescription());
        existing.setCoverImage(incoming.getCoverImage());
        existing.setDeliveryMode(incoming.getDeliveryMode());
        existing.setPrice(incoming.getPrice());
        existing.setOriginalPrice(incoming.getOriginalPrice());
        existing.setStatus(incoming.getStatus());
        existing.setStockCount(incoming.getStockCount() == null ? 0 : incoming.getStockCount());
        existing.setSalesCount(incoming.getSalesCount() == null ? 0 : incoming.getSalesCount());

        return productRepository.save(existing);
    }

    @Transactional
    public void delete(UUID productId) {
        if (!productRepository.existsById(productId)) {
            throw new EntityNotFoundException("商品不存在: " + productId);
        }

        if (productDetailRepository.existsById(productId)) {
            productDetailRepository.deleteById(productId);
        }

        productRepository.deleteById(productId);
    }

    public ProductDetail getDetail(UUID productId) {
        if (!productRepository.existsById(productId)) {
            throw new EntityNotFoundException("商品不存在: " + productId);
        }

        return productDetailRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("商品详情不存在: " + productId));
    }

    @Transactional
    public ProductDetail upsertDetail(UUID productId, UpsertProductDetailRequest request) {
        Product product = getById(productId);

        ProductDetail detail = productDetailRepository.findById(productId)
                .orElseGet(() -> ProductDetail.builder()
                        .product(product)
                        .build());

        detail.setProduct(product);
        detail.setDetailMarkdown(request.detailMarkdown());
        detail.setUsageGuide(request.usageGuide());
        detail.setActivationNotice(request.activationNotice());
        detail.setRefundPolicy(request.refundPolicy());
        detail.setFaqContent(request.faqContent());

        return productDetailRepository.save(detail);
    }

    private void validateCreateOrUpdate(UUID merchantId, UUID categoryId) {
        if (!merchantRepository.existsById(merchantId)) {
            throw new IllegalArgumentException("商家不存在: " + merchantId);
        }

        if (!productCategoryRepository.existsById(categoryId)) {
            throw new IllegalArgumentException("商品分类不存在: " + categoryId);
        }
    }
}