package com.actrade.activationrunner.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "actrade.api")
public class ActradeApiProperties {

    @NotBlank
    private String baseUrl = "http://localhost:8080";

    @NotBlank
    private String internalToken = "change-me";

    @NotBlank
    private String internalTokenHeader = "X-Internal-Token";

    @NotNull
    private Duration connectTimeout = Duration.ofSeconds(5);

    @NotNull
    private Duration readTimeout = Duration.ofSeconds(60);
}