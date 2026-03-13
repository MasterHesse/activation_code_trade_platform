package com.masterhesse.merchant.api;

import com.masterhesse.merchant.api.request.CreateMerchantRequest;
import com.masterhesse.merchant.api.request.UpdateMerchantRequest;
import com.masterhesse.merchant.application.MerchantService;
import com.masterhesse.merchant.domain.Merchant;
import com.masterhesse.merchant.domain.MerchantAuditStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    @PostMapping
    public Merchant create(@Valid @RequestBody CreateMerchantRequest request) {
        Merchant merchant = Merchant.builder()
                .userId(request.userId())
                .merchantName(request.merchantName())
                .merchantType(request.merchantType())
                .contactName(request.contactName())
                .contactEmail(request.contactEmail())
                .contactPhone(request.contactPhone())
                .licenseInfo(request.licenseInfo())
                .auditStatus(MerchantAuditStatus.PENDING)
                .build();

        return merchantService.create(merchant);
    }

    @GetMapping
    public List<Merchant> list() {
        return merchantService.listAll();
    }

    @GetMapping("/{merchantId}")
    public Merchant getById(@PathVariable UUID merchantId) {
        return merchantService.getById(merchantId);
    }

    @GetMapping("/by-user/{userId}")
    public Merchant getByUserId(@PathVariable UUID userId) {
        return merchantService.getByUserId(userId);
    }

    @PutMapping("/{merchantId}")
    public Merchant update(@PathVariable UUID merchantId,
                           @Valid @RequestBody UpdateMerchantRequest request) {
        Merchant merchant = Merchant.builder()
                .merchantName(request.merchantName())
                .merchantType(request.merchantType())
                .contactName(request.contactName())
                .contactEmail(request.contactEmail())
                .contactPhone(request.contactPhone())
                .licenseInfo(request.licenseInfo())
                .auditStatus(request.auditStatus())
                .auditRemark(request.auditRemark())
                .build();

        return merchantService.update(merchantId, merchant);
    }

    @DeleteMapping("/{merchantId}")
    public ResponseEntity<Void> delete(@PathVariable UUID merchantId) {
        merchantService.delete(merchantId);
        return ResponseEntity.noContent().build();
    }
}