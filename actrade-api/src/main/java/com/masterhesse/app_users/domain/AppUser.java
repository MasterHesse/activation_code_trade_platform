package com.masterhesse.app_users.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "app_users",
        indexes = {
                @Index(name = "uk_app_users_username", columnList = "username", unique = true),
                @Index(name = "uk_app_users_email", columnList = "email", unique = true),
                @Index(name = "uk_app_users_phone", columnList = "phone", unique = true)
        }
)
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "username", nullable = false, length = 64)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "nickname", length = 64)
    private String nickname;

    @Column(name = "email", length = 128)
    private String email;

    @Column(name = "phone", length = 32)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private UserStatus status;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (this.role == null) {
            this.role = UserRole.ROLE_USER;
        }
        if (this.status == null) {
            this.status = UserStatus.NORMAL;
        }
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}