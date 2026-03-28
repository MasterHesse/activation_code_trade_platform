package com.masterhesse.activation.persistence;

import com.masterhesse.activation.domain.entity.ActivationTaskArtifact;
import com.masterhesse.activation.domain.enums.ActivationTaskArtifactType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivationTaskArtifactRepository extends JpaRepository<ActivationTaskArtifact, Long> {

    List<ActivationTaskArtifact> findByTaskIdOrderByCreatedAtAsc(Long taskId);

    List<ActivationTaskArtifact> findByAttemptIdOrderByCreatedAtAsc(Long attemptId);

    List<ActivationTaskArtifact> findByTaskIdAndArtifactType(Long taskId, ActivationTaskArtifactType artifactType);
}