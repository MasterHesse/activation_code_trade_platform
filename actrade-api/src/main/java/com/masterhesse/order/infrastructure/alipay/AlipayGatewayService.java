package com.masterhesse.order.infrastructure.alipay;

import com.alipay.easysdk.factory.Factory;
import com.alipay.easysdk.kernel.util.ResponseChecker;
import com.alipay.easysdk.payment.common.models.AlipayTradeCloseResponse;
import com.alipay.easysdk.payment.common.models.AlipayTradeQueryResponse;
import com.alipay.easysdk.payment.page.models.AlipayTradePagePayResponse;
import com.masterhesse.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Slf4j
@Service
public class AlipayGatewayService {

    private final AlipayProperties properties;

    public AlipayGatewayService(AlipayProperties properties) {
        this.properties = properties;
    }

    public boolean enabled() {
        return properties.isEnabled();
    }

    public String buildPagePayForm(String subject, String outTradeNo, BigDecimal totalAmount) {
        requireEnabled();

        if (!StringUtils.hasText(subject)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "支付宝下单失败：subject 不能为空");
        }
        if (!StringUtils.hasText(outTradeNo)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "支付宝下单失败：outTradeNo 不能为空");
        }
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "支付宝下单失败：支付金额必须大于 0");
        }

        String amount = totalAmount.setScale(2, RoundingMode.HALF_UP).toPlainString();

        try {
            AlipayTradePagePayResponse response;

            if (StringUtils.hasText(properties.getNotifyUrl())) {
                response = Factory.Payment.Page()
                        .asyncNotify(properties.getNotifyUrl())
                        .pay(subject, outTradeNo, amount, properties.getReturnUrl());
            } else {
                response = Factory.Payment.Page()
                        .pay(subject, outTradeNo, amount, properties.getReturnUrl());
            }

            if (response == null || !StringUtils.hasText(response.getBody())) {
                throw new BusinessException(HttpStatus.BAD_GATEWAY, "支付宝下单失败：未返回支付表单");
            }

            log.info("Alipay page pay form generated. outTradeNo={}, amount={}", outTradeNo, amount);
            return response.getBody();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Alipay page pay failed. outTradeNo={}, amount={}", outTradeNo, amount, ex);
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "支付宝下单失败: " + ex.getMessage());
        }
    }

    public boolean verifyNotify(Map<String, String> parameters) {
        requireEnabled();

        try {
            return Boolean.TRUE.equals(Factory.Payment.Common().verifyNotify(parameters));
        } catch (Exception ex) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "支付宝回调验签失败: " + ex.getMessage());
        }
    }

    public AlipayTradeQueryResponse queryTrade(String outTradeNo) {
        requireEnabled();

        if (!StringUtils.hasText(outTradeNo)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "支付宝交易查询失败：outTradeNo 不能为空");
        }

        try {
            AlipayTradeQueryResponse response = Factory.Payment.Common().query(outTradeNo);
            if (response == null || !ResponseChecker.success(response)) {
                throw new BusinessException(
                        HttpStatus.BAD_GATEWAY,
                        "支付宝交易查询失败: " + (response == null ? "response is null" : response.getSubMsg())
                );
            }

            log.info("Alipay trade queried. outTradeNo={}, tradeNo={}, tradeStatus={}",
                    outTradeNo, response.getTradeNo(), response.getTradeStatus());

            return response;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Alipay trade query failed. outTradeNo={}", outTradeNo, ex);
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "支付宝交易查询失败: " + ex.getMessage());
        }
    }

    public void closeTrade(String outTradeNo) {
        requireEnabled();

        if (!StringUtils.hasText(outTradeNo)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "支付宝关单失败：outTradeNo 不能为空");
        }

        try {
            AlipayTradeCloseResponse response = Factory.Payment.Common().close(outTradeNo);
            if (response == null || !ResponseChecker.success(response)) {
                throw new BusinessException(
                        HttpStatus.BAD_GATEWAY,
                        "支付宝关单失败: " + (response == null ? "response is null" : response.getSubMsg())
                );
            }

            log.info("Alipay trade closed. outTradeNo={}", outTradeNo);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Alipay trade close failed. outTradeNo={}", outTradeNo, ex);
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "支付宝关单失败: " + ex.getMessage());
        }
    }

    public String buildSubject(String orderNo) {
        String prefix = StringUtils.hasText(properties.getSubjectPrefix())
                ? properties.getSubjectPrefix()
                : "订单";
        return prefix + "-" + orderNo;
    }

    public String getAppId() {
        return properties.getAppId();
    }

    private void requireEnabled() {
        if (!properties.isEnabled()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "支付宝支付未启用");
        }
    }
}