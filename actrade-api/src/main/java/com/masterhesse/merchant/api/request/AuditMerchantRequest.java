package com.masterhesse.merchant.api.request;

import com.masterhesse.merchant.domain.MerchantAuditStatus;

/**
 * 商家审核请求
 * 仅包含审核相关的字段
 */
public record AuditMerchantRequest(
        MerchantAuditStatus auditStatus,

        String auditRemark
) {
}
