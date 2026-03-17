package com.masterhesse.order.api.response;

import com.masterhesse.order.domain.FulfillmentStatus;
import com.masterhesse.order.domain.FulfillmentType;
import com.masterhesse.order.domain.Order;
import com.masterhesse.order.domain.OrderItem;
import com.masterhesse.order.domain.OrderStatus;
import com.masterhesse.order.domain.PaymentMethod;
import com.masterhesse.order.domain.PaymentStatus;
import com.masterhesse.order.domain.SettlementStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class OrderResponse {

    private UUID orderId;
    private String orderNo;
    private UUID userId;
    private UUID merchantId;
    private UUID sellerId;

    private BigDecimal totalAmount;
    private BigDecimal payAmount;
    private BigDecimal platformIncomeAmount;
    private BigDecimal sellerIncomeAmount;

    private OrderStatus orderStatus;
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;

    private FulfillmentType fulfillmentType;
    private FulfillmentStatus fulfillmentStatus;
    private SettlementStatus settlementStatus;

    private LocalDateTime paidAt;
    private LocalDateTime payDeadlineAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime confirmDeadlineAt;
    private LocalDateTime closedAt;

    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<OrderItemResponse> items;

    public static OrderResponse from(Order order, List<OrderItem> orderItems) {
        OrderResponse response = new OrderResponse();
        response.setOrderId(order.getOrderId());
        response.setOrderNo(order.getOrderNo());
        response.setUserId(order.getUserId());
        response.setMerchantId(order.getMerchantId());
        response.setSellerId(order.getSellerId());

        response.setTotalAmount(order.getTotalAmount());
        response.setPayAmount(order.getPayAmount());
        response.setPlatformIncomeAmount(order.getPlatformIncomeAmount());
        response.setSellerIncomeAmount(order.getSellerIncomeAmount());

        response.setOrderStatus(order.getOrderStatus());
        response.setPaymentStatus(order.getPaymentStatus());
        response.setPaymentMethod(order.getPaymentMethod());

        response.setFulfillmentType(order.getFulfillmentType());
        response.setFulfillmentStatus(order.getFulfillmentStatus());
        response.setSettlementStatus(order.getSettlementStatus());

        response.setPaidAt(order.getPaidAt());
        response.setPayDeadlineAt(order.getPayDeadlineAt());
        response.setDeliveredAt(order.getDeliveredAt());
        response.setConfirmDeadlineAt(order.getConfirmDeadlineAt());
        response.setClosedAt(order.getClosedAt());

        response.setRemark(order.getRemark());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());

        response.setItems(orderItems.stream().map(OrderItemResponse::from).toList());
        return response;
    }
}