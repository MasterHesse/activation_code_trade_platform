package com.masterhesse.security.dto;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UUID userId,
        String username,
        String role
) {
    public static AuthResponse of(String accessToken, String refreshToken,
                                   long expiresIn, UUID userId,
                                   String username, String role) {
        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                expiresIn,
                userId,
                username,
                role
        );
    }
}
