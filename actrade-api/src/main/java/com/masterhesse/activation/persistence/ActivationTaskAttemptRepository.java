package com.masterhesse.activation.persistence;

import com.masterhesse.activation.domain.entity.ActivationTaskAttempt;
import com.masterhesse.activation.domain.enums.ActivationTaskAttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActivationTaskAttemptRepository extends JpaRepository<ActivationTaskAttempt, Long> {

    List<ActivationTaskAttempt> findByTaskIdOrderByAttemptNoAsc(Long taskId);

    Optional<ActivationTaskAttempt> findTopByTaskIdOrderByAttemptNoDesc(Long taskId);

    List<ActivationTaskAttempt> findByTaskIdAndStatus(Long taskId, ActivationTaskAttemptStatus status);

    long countByTaskId(Long taskId);
}