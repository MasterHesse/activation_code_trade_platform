// src/main/java/com/masterhesse/activation/persistence/ActivationCodeInventoryRepository.java
package com.masterhesse.activation.persistence;

import com.masterhesse.activation.domain.entity.ActivationCodeInventory;
import com.masterhesse.activation.domain.enums.ActivationCodeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ActivationCodeInventoryRepository extends JpaRepository<ActivationCodeInventory, UUID> {

    boolean existsByProductIdAndCodeValueHash(UUID productId, String codeValueHash);

    Page<ActivationCodeInventory> findByProductId(UUID productId, Pageable pageable);

    Page<ActivationCodeInventory> findByProductIdAndStatus(UUID productId, ActivationCodeStatus status, Pageable pageable);

    List<ActivationCodeInventory> findByBatchNoOrderByCreatedAtDesc(String batchNo);

    /**
     * 查询可用激活码（状态为 AVAILABLE 且未过期）
     */
    @Query("SELECT c FROM ActivationCodeInventory c WHERE c.productId = :productId " +
           "AND c.status = 'AVAILABLE' " +
           "AND (c.expiredAt IS NULL OR c.expiredAt > :now) " +
           "ORDER BY c.createdAt ASC")
    List<ActivationCodeInventory> findAvailableByProductId(
            @Param("productId") UUID productId,
            @Param("now") LocalDateTime now,
            Pageable pageable);

    /**
     * 批量锁定激活码（悲观锁）
     */
    @Modifying
    @Query("UPDATE ActivationCodeInventory c SET c.status = 'LOCKED', " +
           "c.assignedOrderId = :orderId, c.assignedOrderItemId = :orderItemId, " +
           "c.updatedAt = :now " +
           "WHERE c.codeId IN :codeIds AND c.status = 'AVAILABLE'")
    int lockCodes(
            @Param("codeIds") List<UUID> codeIds,
            @Param("orderId") UUID orderId,
            @Param("orderItemId") UUID orderItemId,
            @Param("now") LocalDateTime now);

    /**
     * 批量标记激活码为已售
     */
    @Modifying
    @Query("UPDATE ActivationCodeInventory c SET c.status = 'SOLD', c.updatedAt = :now " +
           "WHERE c.codeId IN :codeIds AND c.status = 'LOCKED'")
    int markAsSold(
            @Param("codeIds") List<UUID> codeIds,
            @Param("now") LocalDateTime now);

    /**
     * 批量释放激活码（从 LOCKED 状态放回 AVAILABLE）
     */
    @Modifying
    @Query("UPDATE ActivationCodeInventory c SET c.status = 'AVAILABLE', " +
           "c.assignedOrderId = NULL, c.assignedOrderItemId = NULL, " +
           "c.updatedAt = :now " +
           "WHERE c.codeId IN :codeIds AND c.status = 'LOCKED'")
    int releaseCodes(
            @Param("codeIds") List<UUID> codeIds,
            @Param("now") LocalDateTime now);

    /**
     * 查询订单已分配的激活码
     */
    List<ActivationCodeInventory> findByAssignedOrderId(UUID orderId);
}