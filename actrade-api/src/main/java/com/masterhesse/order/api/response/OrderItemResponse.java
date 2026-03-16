package com.masterhesse.order.api.response;

import com.masterhesse.order.domain.OrderItem;
import com.masterhesse.product.domain.DeliveryMode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class OrderItemResponse {

    private UUID orderItemId;
    private UUID productId;
    private String productName;
    private DeliveryMode deliveryMode;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal subtotalAmount;
    private LocalDateTime createdAt;

    public static OrderItemResponse from(OrderItem orderItem) {
        OrderItemResponse response = new OrderItemResponse();
        response.setOrderItemId(orderItem.getOrderItemId());
        response.setProductId(orderItem.getProductId());
        response.setProductName(orderItem.getProductName());
        response.setDeliveryMode(orderItem.getDeliveryMode());
        response.setUnitPrice(orderItem.getUnitPrice());
        response.setQuantity(orderItem.getQuantity());
        response.setSubtotalAmount(orderItem.getSubtotalAmount());
        response.setCreatedAt(orderItem.getCreatedAt());
        return response;
    }

    public UUID getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(UUID orderItemId) {
        this.orderItemId = orderItemId;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public DeliveryMode getDeliveryMode() {
        return deliveryMode;
    }

    public void setDeliveryMode(DeliveryMode deliveryMode) {
        this.deliveryMode = deliveryMode;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getSubtotalAmount() {
        return subtotalAmount;
    }

    public void setSubtotalAmount(BigDecimal subtotalAmount) {
        this.subtotalAmount = subtotalAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}