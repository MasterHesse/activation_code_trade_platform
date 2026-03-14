package com.masterhesse.activation.application;

import com.masterhesse.activation.api.request.BatchCreateActivationCodeRequest;
import com.masterhesse.activation.api.request.CreateActivationCodeRequest;
import com.masterhesse.activation.api.request.UpdateActivationCodeStatusRequest;
import com.masterhesse.activation.api.response.ActivationCodeInventoryResponse;
import com.masterhesse.activation.domain.entity.ActivationCodeInventory;
import com.masterhesse.activation.domain.enums.ActivationCodeStatus;
import com.masterhesse.activation.persistence.ActivationCodeInventoryRepository;
import com.masterhesse.product.persistence.ProductRepository; // 按你的实际包名调整
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ActivationCodeInventoryService {

    private final ActivationCodeInventoryRepository activationCodeInventoryRepository;
    private final ProductRepository productRepository;
    private final EntityManager entityManager;

    public ActivationCodeInventoryService(
            ActivationCodeInventoryRepository activationCodeInventoryRepository,
            ProductRepository productRepository,
            EntityManager entityManager
    ) {
        this.activationCodeInventoryRepository = activationCodeInventoryRepository;
        this.productRepository = productRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public ActivationCodeInventoryResponse create(CreateActivationCodeRequest request) {
        validateProductForCode(request.merchantId(), request.productId());

        if (activationCodeInventoryRepository.existsByProductIdAndCodeValueHash(
                request.productId(),
                request.codeValueHash()
        )) {
            throw new IllegalStateException("同一商品下激活码哈希不能重复");
        }

        ActivationCodeInventory saved = activationCodeInventoryRepository.save(
                ActivationCodeInventory.create(
                        request.productId(),
                        request.merchantId(),
                        request.batchNo(),
                        request.codeValueEncrypted(),
                        request.codeValueMasked(),
                        request.codeValueHash(),
                        request.expiredAt(),
                        request.remark()
                )
        );

        entityManager.flush();
        entityManager.refresh(saved);

        return ActivationCodeInventoryResponse.from(saved);
    }

    @Transactional
    public List<ActivationCodeInventoryResponse> batchCreate(BatchCreateActivationCodeRequest request) {
        validateProductForCode(request.merchantId(), request.productId());

        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("批量导入 items 不能为空");
        }

        Set<String> seenHashes = new HashSet<>();

        for (BatchCreateActivationCodeRequest.Item item : request.items()) {
            if (!seenHashes.add(item.codeValueHash())) {
                throw new IllegalArgumentException("批量导入中存在重复 codeValueHash: " + item.codeValueHash());
            }

            if (activationCodeInventoryRepository.existsByProductIdAndCodeValueHash(
                    request.productId(),
                    item.codeValueHash()
            )) {
                throw new IllegalStateException("数据库中已存在重复 codeValueHash: " + item.codeValueHash());
            }
        }

        List<ActivationCodeInventory> entities = request.items().stream()
                .map(item -> ActivationCodeInventory.create(
                        request.productId(),
                        request.merchantId(),
                        item.batchNo(),
                        item.codeValueEncrypted(),
                        item.codeValueMasked(),
                        item.codeValueHash(),
                        item.expiredAt(),
                        item.remark()
                ))
                .toList();

        List<ActivationCodeInventory> savedList = activationCodeInventoryRepository.saveAll(entities);

        entityManager.flush();
        savedList.forEach(entityManager::refresh);

        return savedList.stream()
                .map(ActivationCodeInventoryResponse::from)
                .toList();
    }

    public ActivationCodeInventoryResponse getById(UUID codeId) {
        return ActivationCodeInventoryResponse.from(loadCode(codeId));
    }

    public Page<ActivationCodeInventoryResponse> page(UUID productId, ActivationCodeStatus status, Pageable pageable) {
        Page<ActivationCodeInventory> page;

        if (productId != null && status != null) {
            page = activationCodeInventoryRepository.findByProductIdAndStatus(productId, status, pageable);
        } else if (productId != null) {
            page = activationCodeInventoryRepository.findByProductId(productId, pageable);
        } else {
            page = activationCodeInventoryRepository.findAll(pageable);
        }

        return page.map(ActivationCodeInventoryResponse::from);
    }

    public List<ActivationCodeInventoryResponse> findByBatchNo(String batchNo) {
        return activationCodeInventoryRepository.findByBatchNoOrderByCreatedAtDesc(batchNo)
                .stream()
                .map(ActivationCodeInventoryResponse::from)
                .toList();
    }

    @Transactional
    public ActivationCodeInventoryResponse updateStatus(UUID codeId, UpdateActivationCodeStatusRequest request) {
        ActivationCodeInventory code = loadCode(codeId);
        code.changeStatus(request.status());
        return ActivationCodeInventoryResponse.from(code);
    }

    @Transactional
    public ActivationCodeInventoryResponse voidCode(UUID codeId) {
        ActivationCodeInventory code = loadCode(codeId);
        code.voidCode();
        return ActivationCodeInventoryResponse.from(code);
    }

    @Transactional
    public void delete(UUID codeId) {
        ActivationCodeInventory code = loadCode(codeId);

        if (code.getStatus() == ActivationCodeStatus.LOCKED || code.getStatus() == ActivationCodeStatus.SOLD) {
            throw new IllegalStateException("LOCKED / SOLD 状态的激活码不允许物理删除");
        }

        activationCodeInventoryRepository.delete(code);
    }

    private ActivationCodeInventory loadCode(UUID codeId) {
        return activationCodeInventoryRepository.findById(codeId)
                .orElseThrow(() -> new EntityNotFoundException("激活码库存记录不存在"));
    }

    private void validateProductForCode(UUID merchantId, UUID productId) {
        var product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("商品不存在"));

        if (!Objects.equals(product.getMerchantId(), merchantId)) {
            throw new IllegalArgumentException("商品不属于当前商家");
        }

        if (!"CODE_STOCK".equals(String.valueOf(product.getDeliveryMode()))) {
            throw new IllegalStateException("只有 CODE_STOCK 模式商品才允许导入激活码");
        }
    }
}