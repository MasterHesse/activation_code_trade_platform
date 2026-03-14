// src/main/java/com/masterhesse/activation/api/request/CreateActivationCodeRequest.java
package com.masterhesse.activation.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateActivationCodeRequest(
        @NotNull UUID productId,
        @NotNull UUID merchantId,
        @NotBlank @Size(max = 64) String batchNo,
        @NotBlank String codeValueEncrypted,
        @NotBlank @Size(max = 128) String codeValueMasked,
        @NotBlank @Size(max = 128) String codeValueHash,
        LocalDateTime expiredAt,
        @Size(max = 255) String remark
) {
}