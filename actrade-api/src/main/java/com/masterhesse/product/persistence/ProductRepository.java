package com.masterhesse.product.persistence;

import com.masterhesse.product.domain.Product;
import com.masterhesse.product.domain.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Page<Product> findByMerchantId(UUID merchantId, Pageable pageable);

    Page<Product> findByCategoryId(UUID categoryId, Pageable pageable);

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);
}