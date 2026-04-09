package com.actrade.activationrunner.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "runner.mq")
public class RabbitTopologyProperties {

    @NotBlank
    private String exchange = "activation.task.exchange";

    @NotBlank
    private String queue = "activation.task.execute";

    @NotBlank
    private String routingKey = "activation.task.execute";

    @NotBlank
    private String deadLetterExchange = "activation.task.dlx";

    @NotBlank
    private String deadLetterQueue = "activation.task.execute.dlq";

    @NotBlank
    private String deadLetterRoutingKey = "activation.task.execute.dlq";
}