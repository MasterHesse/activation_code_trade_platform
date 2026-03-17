package com.masterhesse.order.api;

import com.masterhesse.common.api.ApiResponse;
import com.masterhesse.order.api.response.PageResponse;
import com.masterhesse.order.api.response.SellerSettlementResponse;
import com.masterhesse.order.application.settlement.SellerSettlementService;
import com.masterhesse.order.domain.SettlementStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/seller-settlements")
@Validated
public class SellerSettlementController {

    private final SellerSettlementService sellerSettlementService;

    public SellerSettlementController(SellerSettlementService sellerSettlementService) {
        this.sellerSettlementService = sellerSettlementService;
    }

    /**
     * 查询结算单详情
     */
    @GetMapping("/{settlementId}")
    public ApiResponse<SellerSettlementResponse> getSettlement(@PathVariable UUID settlementId) {
        return ApiResponse.success(sellerSettlementService.getSettlement(settlementId));
    }

    /**
     * 按订单查询结算单
     */
    @GetMapping("/by-order/{orderId}")
    public ApiResponse<SellerSettlementResponse> getSettlementByOrderId(@PathVariable UUID orderId) {
        return ApiResponse.success(sellerSettlementService.getSettlementByOrderId(orderId));
    }

    /**
     * 卖家分页查询结算单
     */
    @GetMapping("/seller/{sellerId}/page")
    public ApiResponse<PageResponse<SellerSettlementResponse>> pageSellerSettlements(
            @PathVariable UUID sellerId,
            @RequestParam(required = false) SettlementStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(
                sellerSettlementService.pageSellerSettlements(sellerId, status, page, size)
        );
    }

    /**
     * 商家分页查询结算单
     */
    @GetMapping("/merchant/{merchantId}/page")
    public ApiResponse<PageResponse<SellerSettlementResponse>> pageMerchantSettlements(
            @PathVariable UUID merchantId,
            @RequestParam(required = false) SettlementStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(
                sellerSettlementService.pageMerchantSettlements(merchantId, status, page, size)
        );
    }

    /**
     * 手动补建待结算单
     * 适用于历史数据修复或漏建补偿
     */
    @PostMapping("/orders/{orderId}/prepare")
    public ApiResponse<SellerSettlementResponse> createPendingSettlement(@PathVariable UUID orderId) {
        return ApiResponse.success(sellerSettlementService.createPendingSettlement(orderId));
    }

    /**
     * 手动执行结算
     */
    @PostMapping("/orders/{orderId}/settle")
    public ApiResponse<SellerSettlementResponse> settleOrder(@PathVariable UUID orderId) {
        return ApiResponse.success(sellerSettlementService.settleOrder(orderId));
    }
}