package com.masterhesse.merchant.application;

import com.masterhesse.app_users.domain.AppUser;
import com.masterhesse.app_users.domain.UserRole;
import com.masterhesse.app_users.persistence.AppUserRepository;
import com.masterhesse.merchant.domain.Merchant;
import com.masterhesse.merchant.domain.MerchantAuditStatus;
import com.masterhesse.merchant.persistence.MerchantRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final AppUserRepository appUserRepository;

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
     * 审核商家
     * 当审核通过时，同时将用户角色升级为 ROLE_MERCHANT
     */
    @Transactional
    public Merchant audit(UUID merchantId, MerchantAuditStatus auditStatus, String auditRemark) {
        Merchant existing = getById(merchantId);
        existing.setAuditStatus(auditStatus);
        existing.setAuditRemark(auditRemark);

        // 审核通过时，同步更新用户角色为商家
        if (auditStatus == MerchantAuditStatus.APPROVED) {
            appUserRepository.findById(existing.getUserId()).ifPresent(user -> {
                if (user.getRole() != UserRole.ROLE_MERCHANT) {
                    log.info("Upgrading user {} role from {} to ROLE_MERCHANT after merchant approval",
                            user.getUsername(), user.getRole());
                    user.setRole(UserRole.ROLE_MERCHANT);
                    appUserRepository.save(user);
                }
            });
        }

        return merchantRepository.save(existing);
    }
}