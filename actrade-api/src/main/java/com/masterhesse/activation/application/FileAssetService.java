// src/main/java/com/masterhesse/activation/application/FileAssetService.java
package com.masterhesse.activation.application;

import com.masterhesse.activation.api.request.CreateFileAssetRequest;
import com.masterhesse.activation.api.request.UpdateFileAssetScanRequest;
import com.masterhesse.activation.api.response.FileAssetResponse;
import com.masterhesse.activation.domain.entity.FileAsset;
import com.masterhesse.activation.persistence.ActivationToolVersionRepository;
import com.masterhesse.activation.persistence.FileAssetRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class FileAssetService {

    private final FileAssetRepository fileAssetRepository;
    private final ActivationToolVersionRepository activationToolVersionRepository;

    public FileAssetService(
            FileAssetRepository fileAssetRepository,
            ActivationToolVersionRepository activationToolVersionRepository
    ) {
        this.fileAssetRepository = fileAssetRepository;
        this.activationToolVersionRepository = activationToolVersionRepository;
    }

    @Transactional
    public FileAssetResponse create(CreateFileAssetRequest request) {
        FileAsset saved = fileAssetRepository.save(
                FileAsset.create(
                        request.storageProvider(),
                        request.bucketName(),
                        request.objectKey(),
                        request.originalFilename(),
                        request.storedFilename(),
                        request.contentType(),
                        request.fileSizeBytes(),
                        request.checksumSha256(),
                        request.uploadedBy()
                )
        );
        return FileAssetResponse.from(saved);
    }

    public FileAssetResponse getById(UUID fileId) {
        return FileAssetResponse.from(loadFile(fileId));
    }

    public Page<FileAssetResponse> page(Pageable pageable) {
        return fileAssetRepository.findAll(pageable).map(FileAssetResponse::from);
    }

    public FileAssetResponse getByChecksum(String checksumSha256) {
        FileAsset file = fileAssetRepository.findByChecksumSha256(checksumSha256)
                .orElseThrow(() -> new EntityNotFoundException("文件资源不存在"));
        return FileAssetResponse.from(file);
    }

    @Transactional
    public FileAssetResponse updateScan(UUID fileId, UpdateFileAssetScanRequest request) {
        FileAsset file = loadFile(fileId);
        file.updateScanResult(request.scanStatus(), request.scanReport());
        return FileAssetResponse.from(file);
    }

    @Transactional
    public void delete(UUID fileId) {
        FileAsset file = loadFile(fileId);

        if (activationToolVersionRepository.existsByFile_FileId(fileId)) {
            throw new IllegalStateException("当前文件已被工具版本引用，不能删除");
        }

        fileAssetRepository.delete(file);
    }

    private FileAsset loadFile(UUID fileId) {
        return fileAssetRepository.findById(fileId)
                .orElseThrow(() -> new EntityNotFoundException("文件资源不存在"));
    }
}