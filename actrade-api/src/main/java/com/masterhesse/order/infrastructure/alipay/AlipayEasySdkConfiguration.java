package com.masterhesse.order.infrastructure.alipay;

import com.alipay.easysdk.factory.Factory;
import com.alipay.easysdk.kernel.Config;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Slf4j
@Configuration
@EnableConfigurationProperties(AlipayProperties.class)
@ConditionalOnProperty(prefix = "payment.alipay", name = "enabled", havingValue = "true")
public class AlipayEasySdkConfiguration {

    private final AlipayProperties properties;

    public AlipayEasySdkConfiguration(AlipayProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        validateRequired();

        Config config = new Config();
        config.protocol = properties.getProtocol();
        config.gatewayHost = properties.getGatewayHost();
        config.signType = properties.getSignType();
        config.appId = properties.getAppId();
        config.merchantPrivateKey = properties.getMerchantPrivateKey();
        config.ignoreSSL = properties.isIgnoreSsl();

        boolean certMode =
                StringUtils.hasText(properties.getMerchantCertPath())
                        && StringUtils.hasText(properties.getAlipayCertPath())
                        && StringUtils.hasText(properties.getAlipayRootCertPath());

        if (certMode) {
            config.merchantCertPath = properties.getMerchantCertPath();
            config.alipayCertPath = properties.getAlipayCertPath();
            config.alipayRootCertPath = properties.getAlipayRootCertPath();
        } else {
            config.alipayPublicKey = properties.getAlipayPublicKey();
        }

        if (StringUtils.hasText(properties.getNotifyUrl())) {
            config.notifyUrl = properties.getNotifyUrl();
        }

        Factory.setOptions(config);

        log.info(
                "Alipay Easy SDK initialized. appId={}, gatewayHost={}, notifyUrl={}, returnUrl={}, mode={}",
                mask(properties.getAppId()),
                properties.getGatewayHost(),
                properties.getNotifyUrl(),
                properties.getReturnUrl(),
                certMode ? "CERT" : "PUBLIC_KEY"
        );
    }

    private void validateRequired() {
        if (!StringUtils.hasText(properties.getAppId())) {
            throw new IllegalStateException("payment.alipay.app-id 未配置");
        }

        if (!StringUtils.hasText(properties.getMerchantPrivateKey())) {
            throw new IllegalStateException("payment.alipay.merchant-private-key 未配置");
        }

        if (!StringUtils.hasText(properties.getGatewayHost())) {
            throw new IllegalStateException("payment.alipay.gateway-host 未配置");
        }

        if (!StringUtils.hasText(properties.getReturnUrl())) {
            throw new IllegalStateException("payment.alipay.return-url 未配置");
        }

        boolean certMode =
                StringUtils.hasText(properties.getMerchantCertPath())
                        && StringUtils.hasText(properties.getAlipayCertPath())
                        && StringUtils.hasText(properties.getAlipayRootCertPath());

        boolean keyMode = StringUtils.hasText(properties.getAlipayPublicKey());

        if (!certMode && !keyMode) {
            throw new IllegalStateException(
                    "支付宝配置不完整：证书模式需配置 merchant-cert-path / alipay-cert-path / alipay-root-cert-path；" +
                            "否则至少提供 alipay-public-key"
            );
        }
    }

    private String mask(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        if (value.length() <= 6) {
            return "******";
        }
        return value.substring(0, 3) + "******" + value.substring(value.length() - 3);
    }
}