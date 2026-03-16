package com.masterhesse.order.api.response;

import com.masterhesse.order.domain.Order;
import com.masterhesse.order.domain.OrderStatus;
import com.masterhesse.order.domain.PaymentMethod;
import com.masterhesse.order.domain.PaymentStatus;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class PaymentInitiateResponse {

    private UUID orderId;
    private String orderNo;
    private String paymentRequestNo;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private OrderStatus orderStatus;
    private boolean paid;
    private String message;
    private Map<String, String> channelData = new LinkedHashMap<>();

    public static PaymentInitiateResponse from(Order order,
                                               String message,
                                               Map<String, String> channelData) {
        PaymentInitiateResponse response = new PaymentInitiateResponse();
        response.setOrderId(order.getOrderId());
        response.setOrderNo(order.getOrderNo());
        response.setPaymentRequestNo(order.getPaymentRequestNo());
        response.setPaymentMethod(order.getPaymentMethod());
        response.setPaymentStatus(order.getPaymentStatus());
        response.setOrderStatus(order.getOrderStatus());
        response.setPaid(order.getPaymentStatus() == PaymentStatus.PAID);
        response.setMessage(message);
        response.setChannelData(channelData == null
                ? Collections.emptyMap()
                : new LinkedHashMap<>(channelData));
        return response;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getPaymentRequestNo() {
        return paymentRequestNo;
    }

    public void setPaymentRequestNo(String paymentRequestNo) {
        this.paymentRequestNo = paymentRequestNo;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, String> getChannelData() {
        return channelData;
    }

    public void setChannelData(Map<String, String> channelData) {
        this.channelData = channelData;
    }
}