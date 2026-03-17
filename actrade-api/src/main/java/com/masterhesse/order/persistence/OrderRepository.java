package com.masterhesse.order.persistence;

import com.masterhesse.order.domain.Order;
import com.masterhesse.order.domain.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    boolean existsByOrderNo(String orderNo);

    boolean existsByPaymentRequestNo(String paymentRequestNo);

    Optional<Order> findByPaymentRequestNo(String paymentRequestNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.orderId = :orderId")
    Optional<Order> findByIdForUpdate(@Param("orderId") UUID orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.paymentRequestNo = :paymentRequestNo")
    Optional<Order> findByPaymentRequestNoForUpdate(@Param("paymentRequestNo") String paymentRequestNo);

    @Query("""
            select o.orderId
            from Order o
            where o.orderStatus = com.masterhesse.order.domain.OrderStatus.CREATED
              and o.paymentStatus <> com.masterhesse.order.domain.PaymentStatus.PAID
              and o.payDeadlineAt is not null
              and o.payDeadlineAt <= :now
            """)
    List<UUID> findExpiredUnpaidOrderIds(@Param("now") LocalDateTime now);

    @Query("""
            select o.orderId
            from Order o
            where o.orderStatus = com.masterhesse.order.domain.OrderStatus.DELIVERING
              and o.paymentStatus = com.masterhesse.order.domain.PaymentStatus.PAID
              and o.confirmDeadlineAt is not null
              and o.confirmDeadlineAt <= :now
            """)
    List<UUID> findTimeoutConfirmOrderIds(@Param("now") LocalDateTime now);

    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Order> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);

    Page<Order> findByUserId(UUID userId, Pageable pageable);

    Page<Order> findByUserIdAndOrderStatus(UUID userId, OrderStatus orderStatus, Pageable pageable);

    Page<Order> findByMerchantId(UUID merchantId, Pageable pageable);

    Page<Order> findByMerchantIdAndOrderStatus(UUID merchantId, OrderStatus orderStatus, Pageable pageable);
}