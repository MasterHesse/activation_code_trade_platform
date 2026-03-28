package com.masterhesse.activation.persistence;

import com.masterhesse.activation.domain.entity.ActivationTask;
import com.masterhesse.activation.domain.enums.ActivationTaskStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ActivationTaskRepository extends JpaRepository<ActivationTask, Long> {

    Optional<ActivationTask> findByTaskNo(String taskNo);

    boolean existsByTaskNo(String taskNo);

    Page<ActivationTask> findByStatus(ActivationTaskStatus status, Pageable pageable);

    List<ActivationTask> findByOrderId(Long orderId);

    List<ActivationTask> findByOrderItemId(Long orderItemId);

    List<ActivationTask> findByMerchantId(Long merchantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from ActivationTask t where t.id = :id")
    Optional<ActivationTask> findByIdForUpdate(@Param("id") Long id);
}