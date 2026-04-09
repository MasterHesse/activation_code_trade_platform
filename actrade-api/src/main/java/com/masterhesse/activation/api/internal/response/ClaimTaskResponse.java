package com.masterhesse.activation.api.internal.response;

import com.masterhesse.activation.api.response.ActivationToolVersionResponse;
import com.masterhesse.activation.api.response.FileAssetResponse;

import java.util.UUID;

public record ClaimTaskResponse(
        boolean claimed,
        String reason,

        Long taskId,
        String taskNo,
        Integer attemptNo,
        Long merchantId,

        UUID activationToolId,
        UUID activationToolVersionId,

        String payloadJson,

        ActivationToolVersionResponse toolVersion,
        FileAssetResponse packageAsset
) {

    public static ClaimTaskResponse notClaimed(String reason) {
        return new ClaimTaskResponse(
                false,
                reason,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public static ClaimTaskResponse claimed(
            Long taskId,
            String taskNo,
            Integer attemptNo,
            Long merchantId,
            UUID activationToolId,
            UUID activationToolVersionId,
            String payloadJson,
            ActivationToolVersionResponse toolVersion,
            FileAssetResponse packageAsset
    ) {
        return new ClaimTaskResponse(
                true,
                null,
                taskId,
                taskNo,
                attemptNo,
                merchantId,
                activationToolId,
                activationToolVersionId,
                payloadJson,
                toolVersion,
                packageAsset
        );
    }
}