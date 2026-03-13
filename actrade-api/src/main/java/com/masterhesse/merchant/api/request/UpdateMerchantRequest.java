package com.masterhesse.merchant.api.request;

import com.masterhesse.merchant.domain.MerchantAuditStatus;
import com.masterhesse.merchant.domain.MerchantType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateMerchantRequest(
        @NotBlank
        @Size(max = 128)
        String merchantName,

        @NotNull
        MerchantType merchantType,

        @Size(max = 64)
        String contactName,

        @Email
        @Size(max = 128)
        String contactEmail,

        @Size(max = 32)
        String contactPhone,

        String licenseInfo,

        MerchantAuditStatus auditStatus,

        String auditRemark
) {
}