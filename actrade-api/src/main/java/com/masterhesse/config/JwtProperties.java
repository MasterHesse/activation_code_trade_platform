package com.masterhesse.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    private String secret = "actrade-default-secret-key-please-change-in-production";

    private long accessTokenExpirationMs = 30 * 60 * 1000; // 30 minutes

    private long refreshTokenExpirationMs = 7 * 24 * 60 * 60 * 1000; // 7 days

    private String tokenPrefix = "Bearer ";

    private String headerName = "Authorization";
}
