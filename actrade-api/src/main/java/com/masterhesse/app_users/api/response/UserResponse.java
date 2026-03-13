package com.masterhesse.app_users.api.response;

import com.masterhesse.app_users.domain.AppUser;
import com.masterhesse.app_users.domain.UserRole;
import com.masterhesse.app_users.domain.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID userId,
        String username,
        String nickname,
        String email,
        String phone,
        UserRole role,
        UserStatus status,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static UserResponse from(AppUser user) {
        return new UserResponse(
                user.getUserId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getStatus(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}