// src/main/java/com/masterhesse/activation/application/ActivationToolVersionService.java
package com.masterhesse.activation.application;

import com.masterhesse.activation.api.request.CreateActivationToolVersionRequest;
import com.masterhesse.activation.api.request.UpdateActivationToolVersionManifestRequest;
import com.masterhesse.activation.api.request.UpdateActivationToolVersionReviewRequest;
import com.masterhesse.activation.api.request.UpdateActivationToolVersionScanRequest;
import com.masterhesse.activation.api.request.UpdateActivationToolVersionStatusRequest;
import com.masterhesse.activation.api.response.ActivationToolVersionResponse;
import com.masterhesse.activation.domain.entity.ActivationTool;
import com.masterhesse.activation.domain.entity.ActivationToolVersion;
import com.masterhesse.activation.domain.entity.FileAsset;
import com.masterhesse.activation.domain.enums.ActivationToolVersionStatus;
import com.masterhesse.activation.domain.enums.FileScanStatus;
import com.masterhesse.activation.persistence.ActivationToolRepository;
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
public class ActivationToolVersionService {

    private final ActivationToolVersionRepository activationToolVersionRepository;
    private final ActivationToolRepository activationToolRepository;
    private final FileAssetRepository fileAssetRepository;

    public ActivationToolVersionService(
            ActivationToolVersionRepository activationToolVersionRepository,
            ActivationToolRepository activationToolRepository,
            FileAssetRepository fileAssetRepository
    ) {
        this.activationToolVersionRepository = activationToolVersionRepository;
        this.activationToolRepository = activationToolRepository;
        this.fileAssetRepository = fileAssetRepository;
    }

    @Transactional
    public ActivationToolVersionResponse create(CreateActivationToolVersionRequest request) {
        ActivationTool tool = activationToolRepository.findById(request.toolId())
                .orElseThrow(() -> new EntityNotFoundException("激活工具不存在"));

        FileAsset file = fileAssetRepository.findById(request.fileId())
                .orElseThrow(() -> new EntityNotFoundException("文件资源不存在"));

        if (activationToolVersionRepository.existsByTool_ToolIdAndVersionName(request.toolId(), request.versionName())) {
            throw new IllegalStateException("同一工具下版本号不能重复");
        }

        ActivationToolVersion saved = activationToolVersionRepository.save(
                ActivationToolVersion.create(
                        tool,
                        request.versionName(),
                        file,
                        request.manifestContent(),
                        request.runtimeType(),
                        request.runtimeOs(),
                        request.runtimeArch(),
                        request.entrypoint(),
                        request.execCommand(),
                        request.timeoutSeconds(),
                        request.maxMemoryMb()
                )
        );

        return ActivationToolVersionResponse.from(saved);
    }

    public ActivationToolVersionResponse getById(UUID toolVersionId) {
        return ActivationToolVersionResponse.from(loadVersion(toolVersionId));
    }

    public Page<ActivationToolVersionResponse> page(UUID toolId, Pageable pageable) {
        Page<ActivationToolVersion> page = toolId == null
                ? activationToolVersionRepository.findAll(pageable)
                : activationToolVersionRepository.findByTool_ToolId(toolId, pageable);

        return page.map(ActivationToolVersionResponse::from);
    }

    @Transactional
    public ActivationToolVersionResponse updateStatus(UUID toolVersionId, UpdateActivationToolVersionStatusRequest request) {
        ActivationToolVersion version = loadVersion(toolVersionId);

        if (request.status() == ActivationToolVersionStatus.ENABLED
                && version.getScanStatus() != FileScanStatus.SAFE) {
            throw new IllegalStateException("扫描未通过的版本不能启用");
        }

        boolean isCurrentVersion = activationToolRepository.existsByCurrentVersion_ToolVersionId(toolVersionId);
        if (isCurrentVersion && request.status() != ActivationToolVersionStatus.ENABLED) {
            throw new IllegalStateException("当前生效版本不能直接改为非 ENABLED 状态");
        }

        version.changeStatus(request.status());
        return ActivationToolVersionResponse.from(version);
    }

    @Transactional
    public ActivationToolVersionResponse updateReview(UUID toolVersionId, UpdateActivationToolVersionReviewRequest request) {
        ActivationToolVersion version = loadVersion(toolVersionId);
        version.updateReviewRemark(request.reviewRemark());
        return ActivationToolVersionResponse.from(version);
    }

    @Transactional
    public ActivationToolVersionResponse updateScan(UUID toolVersionId, UpdateActivationToolVersionScanRequest request) {
        ActivationToolVersion version = loadVersion(toolVersionId);
        version.updateScanResult(request.scanStatus(), request.scanReport());
        return ActivationToolVersionResponse.from(version);
    }

    @Transactional
    public ActivationToolVersionResponse updateManifest(UUID toolVersionId, UpdateActivationToolVersionManifestRequest request) {
        ActivationToolVersion version = loadVersion(toolVersionId);
        version.updateManifest(
                request.manifestContent(),
                request.runtimeType(),
                request.runtimeOs(),
                request.runtimeArch(),
                request.entrypoint(),
                request.execCommand(),
                request.timeoutSeconds(),
                request.maxMemoryMb()
        );
        return ActivationToolVersionResponse.from(version);
    }

    @Transactional
    public void delete(UUID toolVersionId) {
        ActivationToolVersion version = loadVersion(toolVersionId);

        boolean isCurrentVersion = activationToolRepository.existsByCurrentVersion_ToolVersionId(toolVersionId);
        if (isCurrentVersion) {
            throw new IllegalStateException("当前生效版本不能删除");
        }

        activationToolVersionRepository.delete(version);
    }

    private ActivationToolVersion loadVersion(UUID toolVersionId) {
        return activationToolVersionRepository.findById(toolVersionId)
                .orElseThrow(() -> new EntityNotFoundException("激活工具版本不存在"));
    }
}