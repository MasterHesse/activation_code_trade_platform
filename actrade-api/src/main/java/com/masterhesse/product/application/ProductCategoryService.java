package com.masterhesse.product.application;

import com.masterhesse.product.domain.ProductCategory;
import com.masterhesse.product.domain.ProductCategoryStatus;
import com.masterhesse.product.persistence.ProductCategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductCategoryService {

    private final ProductCategoryRepository productCategoryRepository;

    @Transactional
    public ProductCategory create(ProductCategory category) {
        validateParent(category.getParentId(), null);

        if (category.getSortNo() == null) {
            category.setSortNo(0);
        }
        if (category.getStatus() == null) {
            category.setStatus(ProductCategoryStatus.ENABLED);
        }

        return productCategoryRepository.save(category);
    }

    public ProductCategory getById(UUID categoryId) {
        return productCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("商品分类不存在: " + categoryId));
    }

    public List<ProductCategory> listAll() {
        return productCategoryRepository.findAllByOrderBySortNoAscCreatedAtAsc();
    }

    public List<ProductCategory> listByStatus(ProductCategoryStatus status) {
        return productCategoryRepository.findByStatusOrderBySortNoAscCreatedAtAsc(status);
    }

    @Transactional
    public ProductCategory update(UUID categoryId, ProductCategory incoming) {
        ProductCategory existing = getById(categoryId);

        validateParent(incoming.getParentId(), categoryId);

        existing.setParentId(incoming.getParentId());
        existing.setName(incoming.getName());
        existing.setSortNo(incoming.getSortNo() == null ? 0 : incoming.getSortNo());
        existing.setStatus(incoming.getStatus() == null ? ProductCategoryStatus.ENABLED : incoming.getStatus());

        return productCategoryRepository.save(existing);
    }

    @Transactional
    public void delete(UUID categoryId) {
        if (!productCategoryRepository.existsById(categoryId)) {
            throw new EntityNotFoundException("商品分类不存在: " + categoryId);
        }
        productCategoryRepository.deleteById(categoryId);
    }

    private void validateParent(UUID parentId, UUID currentId) {
        if (parentId == null) {
            return;
        }

        if (currentId != null && parentId.equals(currentId)) {
            throw new IllegalArgumentException("分类不能将自己设置为父分类");
        }

        if (!productCategoryRepository.existsById(parentId)) {
            throw new IllegalArgumentException("父分类不存在: " + parentId);
        }
    }
}