package com.masterhesse.order.api.request;

import com.masterhesse.order.domain.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public class PayOrderRequest {

    @NotNull(message = "paymentMethod 不能为空")
    private PaymentMethod paymentMethod;

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}