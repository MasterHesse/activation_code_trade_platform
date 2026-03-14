// src/main/java/com/masterhesse/activation/persistence/FileAssetRepository.java
package com.masterhesse.activation.persistence;

import com.masterhesse.activation.domain.entity.FileAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FileAssetRepository extends JpaRepository<FileAsset, UUID> {

    Optional<FileAsset> findByChecksumSha256(String checksumSha256);
}