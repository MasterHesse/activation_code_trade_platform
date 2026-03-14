// src/main/java/com/masterhesse/activation/persistence/ActivationCodeInventoryRepository.java
package com.masterhesse.activation.persistence;

import com.masterhesse.activation.domain.entity.ActivationCodeInventory;
import com.masterhesse.activation.domain.enums.ActivationCodeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ActivationCodeInventoryRepository extends JpaRepository<ActivationCodeInventory, UUID> {

    boolean existsByProductIdAndCodeValueHash(UUID productId, String codeValueHash);

    Page<ActivationCodeInventory> findByProductId(UUID productId, Pageable pageable);

    Page<ActivationCodeInventory> findByProductIdAndStatus(UUID productId, ActivationCodeStatus status, Pageable pageable);

    List<ActivationCodeInventory> findByBatchNoOrderByCreatedAtDesc(String batchNo);
}