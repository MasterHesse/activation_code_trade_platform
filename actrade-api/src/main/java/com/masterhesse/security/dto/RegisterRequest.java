package com.masterhesse.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 64, message = "用户名长度应在 3-64 个字符之间")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 128, message = "密码长度应至少 6 个字符")
        String password,

        String email,

        String phone,

        String nickname
) {
}
