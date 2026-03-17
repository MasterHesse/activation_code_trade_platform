package com.masterhesse.order.application.payment;

import com.masterhesse.common.exception.BusinessException;
import com.masterhesse.order.api.request.PaymentInitiateRequest;
import com.masterhesse.order.domain.Order;
import com.masterhesse.order.domain.PaymentMethod;
import com.masterhesse.order.domain.PaymentStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Component
public class ManualPaymentProcessor implements PaymentProcessor {

    @Override
    public PaymentMethod supportMethod() {
        return PaymentMethod.MANUAL;
    }

    @Override
    public PaymentExecuteResult initiate(Order order, PaymentInitiateRequest request) {
        validateOrder(order);

        String paymentRequestNo = order.getPaymentRequestNo();
        String channelTradeNo = resolveChannelTradeNo(order);

        return PaymentExecuteResult.paid(
                paymentRequestNo,
                channelTradeNo,
                "人工支付确认成功"
        );
    }

    @Override
    public PaymentQueryResult query(Order order) {
        return PaymentQueryResult.of(
                order.getPaymentStatus() == null ? PaymentStatus.UNPAID : order.getPaymentStatus(),
                order.getPaymentRequestNo(),
                resolveChannelTradeNo(order),
                "人工支付本地状态"
        );
    }

    @Override
    public PaymentCloseResult close(Order order) {
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            return PaymentCloseResult.paid("人工支付订单已支付，无需关单");
        }

        return PaymentCloseResult.closed("人工支付订单已关闭");
    }

    private void validateOrder(Order order) {
        if (!StringUtils.hasText(order.getPaymentRequestNo())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "人工支付失败：paymentRequestNo 为空");
        }

        BigDecimal payAmount = order.getPayAmount();
        if (payAmount == null || payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "人工支付失败：payAmount 必须大于 0");
        }
    }

    private String resolveChannelTradeNo(Order order) {
        if (StringUtils.hasText(order.getChannelTradeNo())) {
            return order.getChannelTradeNo();
        }
        if (StringUtils.hasText(order.getPaymentRequestNo())) {
            return "MANUAL" + order.getPaymentRequestNo();
        }
        return null;
    }
}