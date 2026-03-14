// src/main/java/com/masterhesse/activation/api/request/SetCurrentVersionRequest.java
package com.masterhesse.activation.api.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SetCurrentVersionRequest(
        @NotNull UUID currentVersionId
) {
}