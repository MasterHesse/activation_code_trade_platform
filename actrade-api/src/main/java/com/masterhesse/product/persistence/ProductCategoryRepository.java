package com.masterhesse.product.persistence;

import com.masterhesse.product.domain.ProductCategory;
import com.masterhesse.product.domain.ProductCategoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, UUID> {

    List<ProductCategory> findAllByOrderBySortNoAscCreatedAtAsc();

    List<ProductCategory> findByStatusOrderBySortNoAscCreatedAtAsc(ProductCategoryStatus status);
}