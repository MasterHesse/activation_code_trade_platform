package com.masterhesse.order.application.payment;

import com.masterhesse.order.api.request.PaymentInitiateRequest;
import com.masterhesse.order.domain.Order;
import com.masterhesse.order.domain.PaymentMethod;
import org.springframework.stereotype.Component;

@Component
public class MockPaymentProcessor implements PaymentProcessor {

    @Override
    public PaymentMethod supportMethod() {
        return PaymentMethod.MOCK;
    }

    @Override
    public PaymentExecuteResult initiate(Order order, PaymentInitiateRequest request) {
        return PaymentExecuteResult.paid(
                order.getPaymentRequestNo(),
                null,
                "MOCK 支付成功"
        );
    }
}