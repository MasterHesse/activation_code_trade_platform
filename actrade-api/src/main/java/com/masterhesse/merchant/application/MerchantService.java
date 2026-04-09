package com.masterhesse.merchant.application;

import com.masterhesse.merchant.domain.Merchant;
import com.masterhesse.merchant.domain.MerchantAuditStatus;
import com.masterhesse.merchant.persistence.MerchantRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MerchantService {

    private final MerchantRepository merchantRepository;

    @Transactional
    public Merchant create(Merchant merchant) {
        if (merchantRepository.existsByUserId(merchant.getUserId())) {
            throw new IllegalArgumentException("该用户已存在商家记录");
        }

        if (merchant.getAuditStatus() == null) {
            merchant.setAuditStatus(MerchantAuditStatus.PENDING);
        }

        return merchantRepository.save(merchant);
    }

    public Merchant getById(UUID merchantId) {
        return merchantRepository.findById(merchantId)
                .orElseThrow(() -> new EntityNotFoundException("商家不存在: " + merchantId));
    }

    /**
     * 根据用户ID获取商家信息
     * @return 商家信息，如果不存在则返回 null
     */
    public Merchant getByUserId(UUID userId) {
        return merchantRepository.findByUserId(userId).orElse(null);
    }

    public List<Merchant> listAll() {
        return merchantRepository.findAll();
    }

    @Transactional
    public Merchant update(UUID merchantId, Merchant incoming) {
        Merchant existing = getById(merchantId);

        existing.setMerchantName(incoming.getMerchantName());
        existing.setMerchantType(incoming.getMerchantType());
        existing.setContactName(incoming.getContactName());
        existing.setContactEmail(incoming.getContactEmail());
        existing.setContactPhone(incoming.getContactPhone());
        existing.setLicenseInfo(incoming.getLicenseInfo());

        if (incoming.getAuditStatus() != null) {
            existing.setAuditStatus(incoming.getAuditStatus());
        }
        existing.setAuditRemark(incoming.getAuditRemark());

        return merchantRepository.save(existing);
    }

    @Transactional
    public void delete(UUID merchantId) {
        if (!merchantRepository.existsById(merchantId)) {
            throw new EntityNotFoundException("商家不存在: " + merchantId);
        }
        merchantRepository.deleteById(merchantId);
    }

    /**
     * 审核商家（仅更新审核状态和备注）
     */
    @Transactional
    public Merchant audit(UUID merchantId, MerchantAuditStatus auditStatus, String auditRemark) {
        Merchant existing = getById(merchantId);
        existing.setAuditStatus(auditStatus);
        existing.setAuditRemark(auditRemark);
        return merchantRepository.save(existing);
    }
}