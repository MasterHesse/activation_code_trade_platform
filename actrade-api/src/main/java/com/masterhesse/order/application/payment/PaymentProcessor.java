package com.masterhesse.order.application.payment;

import com.masterhesse.common.exception.BusinessException;
import com.masterhesse.order.api.request.PaymentInitiateRequest;
import com.masterhesse.order.domain.Order;
import com.masterhesse.order.domain.PaymentMethod;
import com.masterhesse.order.domain.PaymentStatus;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

import java.util.Map;

public interface PaymentProcessor {

    PaymentMethod supportMethod();

    PaymentExecuteResult initiate(Order order, PaymentInitiateRequest request);

    default PaymentExecuteResult handleCallback(Order order, Map<String, String> callbackParams) {
        throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "当前支付方式不支持异步回调: " + supportMethod()
        );
    }

    default PaymentQueryResult query(Order order) {
        PaymentStatus paymentStatus = order.getPaymentStatus() == null
                ? PaymentStatus.UNPAID
                : order.getPaymentStatus();

        return PaymentQueryResult.of(
                paymentStatus,
                order.getPaymentRequestNo(),
                order.getChannelTradeNo(),
                "返回本地支付状态"
        );
    }

    default PaymentCloseResult close(Order order) {
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            return PaymentCloseResult.paid("订单已支付，无需关单");
        }

        if (order.getPaymentStatus() == PaymentStatus.CLOSED) {
            return PaymentCloseResult.closed("订单已关闭");
        }

        return PaymentCloseResult.closed(
                StringUtils.hasText(order.getPaymentRequestNo())
                        ? "支付单已关闭"
                        : "订单无第三方支付单，已按本地关闭处理"
        );
    }
}