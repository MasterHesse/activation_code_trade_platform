package com.masterhesse.merchant.api;

import com.masterhesse.common.api.ApiResponse;
import com.masterhesse.merchant.api.request.AuditMerchantRequest;
import com.masterhesse.merchant.api.request.CreateMerchantRequest;
import com.masterhesse.merchant.api.request.UpdateMerchantRequest;
import com.masterhesse.merchant.application.MerchantService;
import com.masterhesse.merchant.domain.Merchant;
import com.masterhesse.merchant.domain.MerchantAuditStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<ApiResponse<Merchant>> getByUserId(@PathVariable UUID userId) {
        Merchant merchant = merchantService.getByUserId(userId);
        if (merchant == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<Merchant>error(404, "用户未关联商家", null));
        }
        return ResponseEntity.ok(ApiResponse.success(merchant));
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

    /**
     * 商家审核（管理员专用）
     * 只更新审核状态和备注，不更新其他字段
     */
    @PutMapping("/{merchantId}/audit")
    public Merchant audit(@PathVariable UUID merchantId,
                           @RequestBody AuditMerchantRequest request) {
        return merchantService.audit(merchantId, request.auditStatus(), request.auditRemark());
    }

    @DeleteMapping("/{merchantId}")
    public ResponseEntity<Void> delete(@PathVariable UUID merchantId) {
        merchantService.delete(merchantId);
        return ResponseEntity.noContent().build();
    }
}