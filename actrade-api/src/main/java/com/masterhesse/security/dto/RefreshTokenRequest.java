package com.masterhesse.security.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "Refresh Token 不能为空")
        String refreshToken
) {
}
