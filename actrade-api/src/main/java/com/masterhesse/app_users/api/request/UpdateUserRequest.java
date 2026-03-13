package com.masterhesse.app_users.api.request;

import com.masterhesse.app_users.domain.UserRole;
import com.masterhesse.app_users.domain.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(

        @Size(min = 1, max = 64, message = "username 长度必须在 1-64 之间")
        String username,

        @Size(min = 6, max = 64, message = "password 长度必须在 6-64 之间")
        String password,

        @Size(max = 64, message = "nickname 长度不能超过 64")
        String nickname,

        @Email(message = "email 格式不正确")
        @Size(max = 128, message = "email 长度不能超过 128")
        String email,

        @Size(max = 32, message = "phone 长度不能超过 32")
        String phone,

        UserRole role,

        UserStatus status
) {
}