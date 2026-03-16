package com.masterhesse.order.application.payment;

import com.masterhesse.order.domain.PaymentStatus;

import java.util.LinkedHashMap;
import java.util.Map;

public class PaymentExecuteResult {

    private PaymentStatus paymentStatus;
    private String paymentRequestNo;
    private String channelTradeNo;
    private String message;
    private Map<String, String> channelData = new LinkedHashMap<>();

    public static PaymentExecuteResult paid(String paymentRequestNo,
                                            String channelTradeNo,
                                            String message) {
        PaymentExecuteResult result = new PaymentExecuteResult();
        result.setPaymentStatus(PaymentStatus.PAID);
        result.setPaymentRequestNo(paymentRequestNo);
        result.setChannelTradeNo(channelTradeNo);
        result.setMessage(message);
        return result;
    }

    public static PaymentExecuteResult paying(String paymentRequestNo,
                                              String message,
                                              Map<String, String> channelData) {
        PaymentExecuteResult result = new PaymentExecuteResult();
        result.setPaymentStatus(PaymentStatus.PAYING);
        result.setPaymentRequestNo(paymentRequestNo);
        result.setMessage(message);
        result.setChannelData(channelData == null ? new LinkedHashMap<>() : new LinkedHashMap<>(channelData));
        return result;
    }

    public static PaymentExecuteResult failed(String paymentRequestNo,
                                              String channelTradeNo,
                                              String message) {
        PaymentExecuteResult result = new PaymentExecuteResult();
        result.setPaymentStatus(PaymentStatus.FAILED);
        result.setPaymentRequestNo(paymentRequestNo);
        result.setChannelTradeNo(channelTradeNo);
        result.setMessage(message);
        return result;
    }

    public static PaymentExecuteResult closed(String paymentRequestNo,
                                              String channelTradeNo,
                                              String message) {
        PaymentExecuteResult result = new PaymentExecuteResult();
        result.setPaymentStatus(PaymentStatus.CLOSED);
        result.setPaymentRequestNo(paymentRequestNo);
        result.setChannelTradeNo(channelTradeNo);
        result.setMessage(message);
        return result;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getPaymentRequestNo() {
        return paymentRequestNo;
    }

    public void setPaymentRequestNo(String paymentRequestNo) {
        this.paymentRequestNo = paymentRequestNo;
    }

    public String getChannelTradeNo() {
        return channelTradeNo;
    }

    public void setChannelTradeNo(String channelTradeNo) {
        this.channelTradeNo = channelTradeNo;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getChannelData() {
        return channelData;
    }

    public void setChannelData(Map<String, String> channelData) {
        this.channelData = channelData;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}