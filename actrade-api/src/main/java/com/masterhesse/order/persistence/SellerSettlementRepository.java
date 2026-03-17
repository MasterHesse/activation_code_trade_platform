package com.masterhesse.order.persistence;

import com.masterhesse.order.domain.SellerSettlement;
import com.masterhesse.order.domain.SettlementStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SellerSettlementRepository extends JpaRepository<SellerSettlement, UUID> {

    Optional<SellerSettlement> findByOrderId(UUID orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SellerSettlement s where s.orderId = :orderId")
    Optional<SellerSettlement> findByOrderIdForUpdate(@Param("orderId") UUID orderId);

    boolean existsBySettlementNo(String settlementNo);

    boolean existsByOrderId(UUID orderId);

    List<SellerSettlement> findBySellerIdOrderByCreatedAtDesc(UUID sellerId);

    List<SellerSettlement> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);

    Page<SellerSettlement> findBySellerId(UUID sellerId, Pageable pageable);

    Page<SellerSettlement> findBySellerIdAndSettlementStatus(UUID sellerId,
                                                             SettlementStatus settlementStatus,
                                                             Pageable pageable);

    Page<SellerSettlement> findByMerchantId(UUID merchantId, Pageable pageable);

    Page<SellerSettlement> findByMerchantIdAndSettlementStatus(UUID merchantId,
                                                               SettlementStatus settlementStatus,
                                                               Pageable pageable);
}