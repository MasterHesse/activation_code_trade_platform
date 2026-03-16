package com.masterhesse.order.application.payment;

import com.masterhesse.order.api.request.PaymentInitiateRequest;
import com.masterhesse.order.domain.Order;
import com.masterhesse.order.domain.PaymentMethod;
import com.masterhesse.common.exception.BusinessException;

import java.util.Map;

public interface PaymentProcessor {

    PaymentMethod supportMethod();

    PaymentExecuteResult initiate(Order order, PaymentInitiateRequest request);

    default PaymentExecuteResult handleCallback(Order order, Map<String, String> callbackParams) {
        throw new BusinessException("当前支付方式不支持回调处理");
    }
}