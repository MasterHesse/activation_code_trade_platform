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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
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
        Page<Product> result;
        if (merchantId != null) {
            result = productRepository.findByMerchantId(merchantId, pageable);
            log.debug("查询商家商品 - merchantId: {}, 结果数量: {}", merchantId, result.getContent().size());
            // 打印第一个商品的库存用于调试
            if (!result.getContent().isEmpty()) {
                Product first = result.getContent().get(0);
                log.debug("第一个商品 - productId: {}, name: {}, stockCount: {}",
                        first.getProductId(), first.getName(), first.getStockCount());
            }
            return result;
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

        // 记录更新前的库存值，用于调试
        Integer oldStockCount = existing.getStockCount();

        // 只更新非空字段（支持部分更新）
        if (incoming.getCategoryId() != null) {
            if (!productCategoryRepository.existsById(incoming.getCategoryId())) {
                throw new IllegalArgumentException("商品分类不存在: " + incoming.getCategoryId());
            }
            existing.setCategoryId(incoming.getCategoryId());
        }
        if (incoming.getName() != null) {
            existing.setName(incoming.getName());
        }
        if (incoming.getSubtitle() != null) {
            existing.setSubtitle(incoming.getSubtitle());
        }
        if (incoming.getDescription() != null) {
            existing.setDescription(incoming.getDescription());
        }
        if (incoming.getCoverImage() != null) {
            existing.setCoverImage(incoming.getCoverImage());
        }
        if (incoming.getDeliveryMode() != null) {
            existing.setDeliveryMode(incoming.getDeliveryMode());
        }
        if (incoming.getPrice() != null) {
            existing.setPrice(incoming.getPrice());
        }
        if (incoming.getOriginalPrice() != null) {
            existing.setOriginalPrice(incoming.getOriginalPrice());
        }
        if (incoming.getStatus() != null) {
            existing.setStatus(incoming.getStatus());
        }
        // 注意：stockCount 和 salesCount 不在部分更新范围内，保持原值
        // 只有明确传递了新值才更新
        if (incoming.getStockCount() != null) {
            existing.setStockCount(incoming.getStockCount());
        }
        if (incoming.getSalesCount() != null) {
            existing.setSalesCount(incoming.getSalesCount());
        }

        Product saved = productRepository.save(existing);

        // 记录更新后的库存值，用于调试
        log.debug("商品更新 - productId: {}, 更新前stockCount: {}, 更新后stockCount: {}, 传入stockCount: {}",
                productId, oldStockCount, saved.getStockCount(), incoming.getStockCount());

        return saved;
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
                .orElse(null); // 商品详情可选，不存在时返回 null 而不是抛异常
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