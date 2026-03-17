package com.masterhesse.order.application.payment;

import com.alipay.easysdk.payment.common.models.AlipayTradeQueryResponse;
import com.masterhesse.common.exception.BusinessException;
import com.masterhesse.order.api.request.PaymentInitiateRequest;
import com.masterhesse.order.domain.Order;
import com.masterhesse.order.domain.PaymentMethod;
import com.masterhesse.order.domain.PaymentStatus;
import com.masterhesse.order.infrastructure.alipay.AlipayGatewayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
public class AlipayPaymentProcessor implements PaymentProcessor {

    private final AlipayGatewayService alipayGatewayService;

    public AlipayPaymentProcessor(AlipayGatewayService alipayGatewayService) {
        this.alipayGatewayService = alipayGatewayService;
    }

    @Override
    public PaymentMethod supportMethod() {
        return PaymentMethod.ALIPAY;
    }

    @Override
    public PaymentExecuteResult initiate(Order order, PaymentInitiateRequest request) {
        if (!StringUtils.hasText(order.getPaymentRequestNo())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "支付发起失败：paymentRequestNo 为空");
        }

        BigDecimal payAmount = order.getPayAmount();
        if (payAmount == null || payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "支付发起失败：payAmount 必须大于 0");
        }

        String outTradeNo = order.getPaymentRequestNo();
        String formHtml = alipayGatewayService.buildPagePayForm(
                alipayGatewayService.buildSubject(order.getOrderNo()),
                outTradeNo,
                payAmount
        );

        Map<String, String> channelData = new LinkedHashMap<>();
        channelData.put("gateway", "ALIPAY_PAGE");
        channelData.put("appId", alipayGatewayService.getAppId());
        channelData.put("outTradeNo", outTradeNo);
        channelData.put("orderNo", order.getOrderNo());
        channelData.put("amount", payAmount.setScale(2, RoundingMode.HALF_UP).toPlainString());
        channelData.put("formHtml", formHtml);

        log.info("Alipay initiate success. orderNo={}, paymentRequestNo={}, amount={}",
                order.getOrderNo(), outTradeNo, channelData.get("amount"));

        return PaymentExecuteResult.paying(
                outTradeNo,
                "支付宝支付单已生成，请跳转收银台完成支付",
                channelData
        );
    }

    @Override
    public PaymentExecuteResult handleCallback(Order order, Map<String, String> callbackParams) {
        if (!alipayGatewayService.verifyNotify(callbackParams)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "支付宝回调验签失败");
        }

        String outTradeNo = callbackParams.get("out_trade_no");
        String appId = callbackParams.get("app_id");
        String totalAmount = callbackParams.get("total_amount");

        if (!StringUtils.hasText(outTradeNo)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "支付宝回调缺少 out_trade_no");
        }

        if (!Objects.equals(order.getPaymentRequestNo(), outTradeNo)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "支付宝回调订单号不匹配");
        }

        if (StringUtils.hasText(appId) && !Objects.equals(alipayGatewayService.getAppId(), appId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "支付宝回调 app_id 不匹配");
        }

        if (StringUtils.hasText(totalAmount)) {
            BigDecimal notifyAmount = new BigDecimal(totalAmount).setScale(2, RoundingMode.HALF_UP);
            BigDecimal orderAmount = order.getPayAmount().setScale(2, RoundingMode.HALF_UP);
            if (orderAmount.compareTo(notifyAmount) != 0) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "支付宝回调金额不匹配");
            }
        }

        AlipayTradeQueryResponse query = alipayGatewayService.queryTrade(outTradeNo);

        if (!Objects.equals(query.getOutTradeNo(), outTradeNo)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "支付宝查询结果订单号不匹配");
        }

        if (StringUtils.hasText(query.getTotalAmount())) {
            BigDecimal queryAmount = new BigDecimal(query.getTotalAmount()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal orderAmount = order.getPayAmount().setScale(2, RoundingMode.HALF_UP);
            if (orderAmount.compareTo(queryAmount) != 0) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "支付宝查询结果金额不匹配");
            }
        }

        String tradeStatus = query.getTradeStatus();
        log.info("Alipay callback verified. orderNo={}, paymentRequestNo={}, tradeNo={}, tradeStatus={}",
                order.getOrderNo(), outTradeNo, query.getTradeNo(), tradeStatus);

        return switch (tradeStatus) {
            case "TRADE_SUCCESS", "TRADE_FINISHED" ->
                    PaymentExecuteResult.paid(outTradeNo, query.getTradeNo(), "支付宝支付成功");

            case "WAIT_BUYER_PAY" ->
                    PaymentExecuteResult.paying(outTradeNo, "支付宝订单待支付", Map.of(
                            "tradeStatus", tradeStatus
                    ));

            case "TRADE_CLOSED" ->
                    PaymentExecuteResult.closed(outTradeNo, query.getTradeNo(), "支付宝交易已关闭");

            default ->
                    PaymentExecuteResult.failed(outTradeNo, query.getTradeNo(), "支付宝交易状态: " + tradeStatus);
        };
    }

    @Override
    public PaymentQueryResult query(Order order) {
        if (!StringUtils.hasText(order.getPaymentRequestNo())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "支付宝交易查询失败：paymentRequestNo 为空");
        }

        AlipayTradeQueryResponse response = alipayGatewayService.queryTrade(order.getPaymentRequestNo());
        String tradeStatus = response.getTradeStatus();
        String tradeNo = response.getTradeNo();

        return switch (tradeStatus) {
            case "TRADE_SUCCESS", "TRADE_FINISHED" ->
                    PaymentQueryResult.of(PaymentStatus.PAID, order.getPaymentRequestNo(), tradeNo, "支付宝交易已支付");

            case "TRADE_CLOSED" ->
                    PaymentQueryResult.of(PaymentStatus.CLOSED, order.getPaymentRequestNo(), tradeNo, "支付宝交易已关闭");

            case "WAIT_BUYER_PAY" ->
                    PaymentQueryResult.of(PaymentStatus.PAYING, order.getPaymentRequestNo(), tradeNo, "支付宝交易待支付");

            default ->
                    PaymentQueryResult.of(PaymentStatus.FAILED, order.getPaymentRequestNo(), tradeNo, "支付宝交易状态: " + tradeStatus);
        };
    }

    @Override
    public PaymentCloseResult close(Order order) {
        if (!StringUtils.hasText(order.getPaymentRequestNo())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "支付宝关单失败：paymentRequestNo 为空");
        }

        PaymentQueryResult queryResult = query(order);

        if (queryResult.paymentStatus() == PaymentStatus.PAID) {
            return PaymentCloseResult.paid("支付宝交易已支付，不能关单");
        }

        if (queryResult.paymentStatus() == PaymentStatus.CLOSED) {
            return PaymentCloseResult.closed("支付宝交易已关闭");
        }

        alipayGatewayService.closeTrade(order.getPaymentRequestNo());

        log.info("Alipay trade closed by processor. orderNo={}, paymentRequestNo={}",
                order.getOrderNo(), order.getPaymentRequestNo());

        return PaymentCloseResult.closed("支付宝交易已关闭");
    }
}