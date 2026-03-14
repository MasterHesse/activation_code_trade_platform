// src/main/java/com/masterhesse/activation/persistence/ActivationToolVersionRepository.java
package com.masterhesse.activation.persistence;

import com.masterhesse.activation.domain.entity.ActivationToolVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ActivationToolVersionRepository extends JpaRepository<ActivationToolVersion, UUID> {

    boolean existsByTool_ToolIdAndVersionName(UUID toolId, String versionName);

    Page<ActivationToolVersion> findByTool_ToolId(UUID toolId, Pageable pageable);

    long countByTool_ToolId(UUID toolId);

    boolean existsByFile_FileId(UUID fileId);
}