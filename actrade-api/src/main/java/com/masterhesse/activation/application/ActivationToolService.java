package com.masterhesse.activation.application;

import com.masterhesse.activation.api.request.CreateActivationToolRequest;
import com.masterhesse.activation.api.request.SetCurrentVersionRequest;
import com.masterhesse.activation.api.request.UpdateActivationToolAuditStatusRequest;
import com.masterhesse.activation.api.request.UpdateActivationToolNameRequest;
import com.masterhesse.activation.api.request.UpdateActivationToolStatusRequest;
import com.masterhesse.activation.api.response.ActivationToolResponse;
import com.masterhesse.activation.domain.entity.ActivationTool;
import com.masterhesse.activation.domain.entity.ActivationToolVersion;
import com.masterhesse.activation.domain.enums.ActivationToolVersionStatus;
import com.masterhesse.activation.persistence.ActivationToolRepository;
import com.masterhesse.activation.persistence.ActivationToolVersionRepository;
import com.masterhesse.product.persistence.ProductRepository; // 按你的实际包名调整
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ActivationToolService {

    private final ActivationToolRepository activationToolRepository;
    private final ActivationToolVersionRepository activationToolVersionRepository;
    private final ProductRepository productRepository;
    private final EntityManager entityManager;

    public ActivationToolService(
            ActivationToolRepository activationToolRepository,
            ActivationToolVersionRepository activationToolVersionRepository,
            ProductRepository productRepository,
            EntityManager entityManager
    ) {
        this.activationToolRepository = activationToolRepository;
        this.activationToolVersionRepository = activationToolVersionRepository;
        this.productRepository = productRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public ActivationToolResponse create(CreateActivationToolRequest request) {
        validateProductForTool(request.merchantId(), request.productId());

        if (activationToolRepository.existsByProductId(request.productId())) {
            throw new IllegalStateException("该商品已经绑定过激活工具");
        }

        ActivationTool saved = activationToolRepository.save(
                ActivationTool.create(request.merchantId(), request.productId(), request.toolName())
        );

        entityManager.flush();
        entityManager.refresh(saved);

        return ActivationToolResponse.from(saved);
    }

    public ActivationToolResponse getById(UUID toolId) {
        return ActivationToolResponse.from(loadTool(toolId));
    }

    public Page<ActivationToolResponse> page(UUID merchantId, Pageable pageable) {
        Page<ActivationTool> page = merchantId == null
                ? activationToolRepository.findAll(pageable)
                : activationToolRepository.findByMerchantId(merchantId, pageable);

        return page.map(ActivationToolResponse::from);
    }

    @Transactional
    public ActivationToolResponse updateName(UUID toolId, UpdateActivationToolNameRequest request) {
        ActivationTool tool = loadTool(toolId);
        tool.rename(request.toolName());
        return ActivationToolResponse.from(tool);
    }

    @Transactional
    public ActivationToolResponse updateStatus(UUID toolId, UpdateActivationToolStatusRequest request) {
        ActivationTool tool = loadTool(toolId);
        tool.changeToolStatus(request.toolStatus());
        return ActivationToolResponse.from(tool);
    }

    @Transactional
    public ActivationToolResponse updateAuditStatus(UUID toolId, UpdateActivationToolAuditStatusRequest request) {
        ActivationTool tool = loadTool(toolId);
        tool.changeAuditStatus(request.auditStatus());
        return ActivationToolResponse.from(tool);
    }

    @Transactional
    public ActivationToolResponse setCurrentVersion(UUID toolId, SetCurrentVersionRequest request) {
        ActivationTool tool = loadTool(toolId);
        ActivationToolVersion version = activationToolVersionRepository.findById(request.currentVersionId())
                .orElseThrow(() -> new EntityNotFoundException("激活工具版本不存在"));

        if (!version.getToolId().equals(toolId)) {
            throw new IllegalArgumentException("该版本不属于当前工具");
        }

        if (version.getStatus() != ActivationToolVersionStatus.ENABLED) {
            throw new IllegalStateException("只有 ENABLED 状态的版本允许设置为当前生效版本");
        }

        tool.changeCurrentVersion(version);
        return ActivationToolResponse.from(tool);
    }

    @Transactional
    public void delete(UUID toolId) {
        ActivationTool tool = loadTool(toolId);

        long versionCount = activationToolVersionRepository.countByTool_ToolId(toolId);
        if (versionCount > 0) {
            throw new IllegalStateException("当前工具下仍存在版本记录，不能直接删除");
        }

        activationToolRepository.delete(tool);
    }

    private ActivationTool loadTool(UUID toolId) {
        return activationToolRepository.findById(toolId)
                .orElseThrow(() -> new EntityNotFoundException("激活工具不存在"));
    }

    private void validateProductForTool(UUID merchantId, UUID productId) {
        var product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("商品不存在"));

        if (!Objects.equals(product.getMerchantId(), merchantId)) {
            throw new IllegalArgumentException("商品不属于当前商家");
        }

        if (!"TOOL_EXECUTION".equals(String.valueOf(product.getDeliveryMode()))) {
            throw new IllegalStateException("只有 TOOL_EXECUTION 模式商品才允许绑定激活工具");
        }
    }

    @Transactional
    public ActivationToolResponse clearCurrentVersion(UUID toolId) {
        ActivationTool tool = loadTool(toolId);
        tool.changeCurrentVersion(null);
        return ActivationToolResponse.from(tool);
    }
}