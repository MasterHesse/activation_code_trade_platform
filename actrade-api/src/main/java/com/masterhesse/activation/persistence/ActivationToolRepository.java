// src/main/java/com/masterhesse/activation/persistence/ActivationToolRepository.java
package com.masterhesse.activation.persistence;

import com.masterhesse.activation.domain.entity.ActivationTool;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ActivationToolRepository extends JpaRepository<ActivationTool, UUID> {

    boolean existsByProductId(UUID productId);

    boolean existsByCurrentVersion_ToolVersionId(UUID toolVersionId);

    Page<ActivationTool> findByMerchantId(UUID merchantId, Pageable pageable);
}