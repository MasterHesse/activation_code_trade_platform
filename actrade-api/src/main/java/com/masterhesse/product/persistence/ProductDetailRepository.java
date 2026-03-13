package com.masterhesse.product.persistence;

import com.masterhesse.product.domain.ProductDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductDetailRepository extends JpaRepository<ProductDetail, UUID> {
}