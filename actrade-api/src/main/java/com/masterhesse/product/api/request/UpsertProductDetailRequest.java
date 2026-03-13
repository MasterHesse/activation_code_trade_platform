package com.masterhesse.product.api.request;

public record UpsertProductDetailRequest(
        String detailMarkdown,
        String usageGuide,
        String activationNotice,
        String refundPolicy,
        String faqContent
) {
}