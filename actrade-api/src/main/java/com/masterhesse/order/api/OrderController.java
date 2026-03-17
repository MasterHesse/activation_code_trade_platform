package com.masterhesse.order.api;

import com.masterhesse.common.api.ApiResponse;
import com.masterhesse.order.api.request.PayOrderRequest;
import com.masterhesse.order.api.request.PaymentInitiateRequest;
import com.masterhesse.order.api.request.SubmitOrderRequest;
import com.masterhesse.order.api.response.OrderResponse;
import com.masterhesse.order.api.response.OrderSummaryResponse;
import com.masterhesse.order.api.response.PageResponse;
import com.masterhesse.order.api.response.PaymentInitiateResponse;
import com.masterhesse.order.application.OrderPaymentService;
import com.masterhesse.order.application.OrderService;
import com.masterhesse.order.domain.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@Validated
public class OrderController {

    private final OrderService orderService;
    private final OrderPaymentService orderPaymentService;

    public OrderController(OrderService orderService,
                           OrderPaymentService orderPaymentService) {
        this.orderService = orderService;
        this.orderPaymentService = orderPaymentService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> submitOrder(@Valid @RequestBody SubmitOrderRequest request) {
        OrderResponse response = orderService.submitOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @PostMapping("/{orderId}/cancel")
    public ApiResponse<OrderResponse> cancelOrder(@PathVariable UUID orderId) {
        return ApiResponse.success(orderService.cancelOrder(orderId));
    }

    /**
     * 兼容老接口：MOCK / MANUAL
     */
    @PostMapping("/{orderId}/pay")
    public ApiResponse<OrderResponse> payOrder(@PathVariable UUID orderId,
                                               @Valid @RequestBody PayOrderRequest request) {
        return ApiResponse.success(orderService.payOrder(orderId, request));
    }

    /**
     * 新支付发起接口：MOCK / MANUAL / ALIPAY
     */
    @PostMapping("/{orderId}/payment-initiate")
    public ApiResponse<PaymentInitiateResponse> initiatePayment(@PathVariable UUID orderId,
                                                                @Valid @RequestBody PaymentInitiateRequest request) {
        return ApiResponse.success(orderPaymentService.initiatePayment(orderId, request));
    }

    @PostMapping("/{orderId}/deliver")
    public ApiResponse<OrderResponse> deliverOrder(@PathVariable UUID orderId) {
        return ApiResponse.success(orderService.deliverOrder(orderId));
    }

    @PostMapping("/{orderId}/delivery-failed")
    public ApiResponse<OrderResponse> markDeliveryFailed(@PathVariable UUID orderId) {
        return ApiResponse.success(orderService.markDeliveryFailed(orderId));
    }

    @PostMapping("/{orderId}/confirm-receipt")
    public ApiResponse<OrderResponse> confirmReceipt(@PathVariable UUID orderId) {
        return ApiResponse.success(orderService.confirmReceipt(orderId));
    }

    /**
     * 手动补建待结算记录
     * 适用于：
     * 1. 订单已完成但结算单没创建
     * 2. 历史数据修复
     */
    @PostMapping("/{orderId}/settlement/prepare")
    public ApiResponse<OrderResponse> prepareSettlement(@PathVariable UUID orderId) {
        return ApiResponse.success(orderService.prepareSettlement(orderId));
    }

    /**
     * 手动标记订单结算完成
     */
    @PostMapping("/{orderId}/settlement/settle")
    public ApiResponse<OrderResponse> markSettlementSettled(@PathVariable UUID orderId) {
        return ApiResponse.success(orderService.markSettlementSettled(orderId));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> getOrder(@PathVariable UUID orderId) {
        return ApiResponse.success(orderService.getOrder(orderId));
    }

    @GetMapping
    public ApiResponse<List<OrderSummaryResponse>> listUserOrders(@RequestParam UUID userId) {
        return ApiResponse.success(orderService.listUserOrders(userId));
    }

    @GetMapping("/merchant/{merchantId}")
    public ApiResponse<List<OrderSummaryResponse>> listMerchantOrders(@PathVariable UUID merchantId) {
        return ApiResponse.success(orderService.listMerchantOrders(merchantId));
    }

    @GetMapping("/page")
    public ApiResponse<PageResponse<OrderSummaryResponse>> pageUserOrders(@RequestParam UUID userId,
                                                                          @RequestParam(required = false) OrderStatus status,
                                                                          @RequestParam(defaultValue = "0") @Min(0) int page,
                                                                          @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(orderService.pageUserOrders(userId, status, page, size));
    }

    @GetMapping("/merchant/{merchantId}/page")
    public ApiResponse<PageResponse<OrderSummaryResponse>> pageMerchantOrders(@PathVariable UUID merchantId,
                                                                              @RequestParam(required = false) OrderStatus status,
                                                                              @RequestParam(defaultValue = "0") @Min(0) int page,
                                                                              @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(orderService.pageMerchantOrders(merchantId, status, page, size));
    }
}