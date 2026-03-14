// src/main/java/com/masterhesse/activation/api/request/UpdateActivationToolVersionScanRequest.java
package com.masterhesse.activation.api.request;

import com.masterhesse.activation.domain.enums.FileScanStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateActivationToolVersionScanRequest(
        @NotNull FileScanStatus scanStatus,
        String scanReport
) {
}