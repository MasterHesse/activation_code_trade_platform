// src/main/java/com/masterhesse/activation/api/request/BatchCreateActivationCodeRequest.java
package com.masterhesse.activation.api.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record BatchCreateActivationCodeRequest(
        @NotNull UUID productId,
        @NotNull UUID merchantId,
        @NotEmpty List<@Valid Item> items
) {
    public record Item(
            @NotBlank @Size(max = 64) String batchNo,
            @NotBlank String codeValueEncrypted,
            @NotBlank @Size(max = 128) String codeValueMasked,
            @NotBlank @Size(max = 128) String codeValueHash,
            LocalDateTime expiredAt,
            @Size(max = 255) String remark
    ) {
    }
}