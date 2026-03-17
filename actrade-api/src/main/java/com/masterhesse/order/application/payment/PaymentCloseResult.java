package com.masterhesse.order.application.payment;

public record PaymentCloseResult(
        boolean paid,
        boolean closed,
        String message
) {

    public static PaymentCloseResult paid(String message) {
        return new PaymentCloseResult(true, false, message);
    }

    public static PaymentCloseResult closed(String message) {
        return new PaymentCloseResult(false, true, message);
    }

    public boolean isPaid() {
        return paid;
    }

    public boolean isClosed() {
        return closed;
    }
}