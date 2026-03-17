package com.masterhesse.order.api.response;

import com.masterhesse.order.domain.SellerSettlement;
import com.masterhesse.order.domain.SettlementStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class SellerSettlementResponse {

    private UUID settlementId;
    private String settlementNo;
    private UUID orderId;
    private UUID sellerId;
    private UUID merchantId;
    private BigDecimal settlementAmount;
    private SettlementStatus settlementStatus;
    private LocalDateTime settledAt;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SellerSettlementResponse from(SellerSettlement settlement) {
        if (settlement == null) {
            return null;
        }

        SellerSettlementResponse response = new SellerSettlementResponse();
        response.setSettlementId(settlement.getSettlementId());
        response.setSettlementNo(settlement.getSettlementNo());
        response.setOrderId(settlement.getOrderId());
        response.setSellerId(settlement.getSellerId());
        response.setMerchantId(settlement.getMerchantId());
        response.setSettlementAmount(settlement.getSettlementAmount());
        response.setSettlementStatus(settlement.getSettlementStatus());
        response.setSettledAt(settlement.getSettledAt());
        response.setRemark(settlement.getRemark());
        response.setCreatedAt(settlement.getCreatedAt());
        response.setUpdatedAt(settlement.getUpdatedAt());
        return response;
    }
}