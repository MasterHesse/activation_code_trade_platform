package com.masterhesse.order.application.payment;

import com.masterhesse.order.domain.PaymentStatus;

public record PaymentQueryResult(
        PaymentStatus paymentStatus,
        String paymentRequestNo,
        String channelTradeNo,
        String message
) {

    public static PaymentQueryResult of(PaymentStatus paymentStatus,
                                        String paymentRequestNo,
                                        String channelTradeNo,
                                        String message) {
        return new PaymentQueryResult(paymentStatus, paymentRequestNo, channelTradeNo, message);
    }

    public boolean isPaid() {
        return paymentStatus == PaymentStatus.PAID;
    }

    public boolean isClosed() {
        return paymentStatus == PaymentStatus.CLOSED;
    }
}