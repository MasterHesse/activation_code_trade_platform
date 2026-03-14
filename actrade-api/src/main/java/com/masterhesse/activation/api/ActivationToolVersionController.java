// src/main/java/com/masterhesse/activation/api/ActivationToolVersionController.java
package com.masterhesse.activation.api;

import com.masterhesse.activation.api.request.CreateActivationToolVersionRequest;
import com.masterhesse.activation.api.request.UpdateActivationToolVersionManifestRequest;
import com.masterhesse.activation.api.request.UpdateActivationToolVersionReviewRequest;
import com.masterhesse.activation.api.request.UpdateActivationToolVersionScanRequest;
import com.masterhesse.activation.api.request.UpdateActivationToolVersionStatusRequest;
import com.masterhesse.activation.api.response.ActivationToolVersionResponse;
import com.masterhesse.activation.application.ActivationToolVersionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/activation-tool-versions")
public class ActivationToolVersionController {

    private final ActivationToolVersionService activationToolVersionService;

    public ActivationToolVersionController(ActivationToolVersionService activationToolVersionService) {
        this.activationToolVersionService = activationToolVersionService;
    }

    @PostMapping
    public ResponseEntity<ActivationToolVersionResponse> create(
            @Valid @RequestBody CreateActivationToolVersionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(activationToolVersionService.create(request));
    }

    @GetMapping("/{toolVersionId}")
    public ActivationToolVersionResponse getById(@PathVariable UUID toolVersionId) {
        return activationToolVersionService.getById(toolVersionId);
    }

    @GetMapping
    public Page<ActivationToolVersionResponse> page(
            @RequestParam(required = false) UUID toolId,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return activationToolVersionService.page(toolId, pageable);
    }

    @PutMapping("/{toolVersionId}/status")
    public ActivationToolVersionResponse updateStatus(
            @PathVariable UUID toolVersionId,
            @Valid @RequestBody UpdateActivationToolVersionStatusRequest request
    ) {
        return activationToolVersionService.updateStatus(toolVersionId, request);
    }

    @PutMapping("/{toolVersionId}/review")
    public ActivationToolVersionResponse updateReview(
            @PathVariable UUID toolVersionId,
            @Valid @RequestBody UpdateActivationToolVersionReviewRequest request
    ) {
        return activationToolVersionService.updateReview(toolVersionId, request);
    }

    @PutMapping("/{toolVersionId}/scan")
    public ActivationToolVersionResponse updateScan(
            @PathVariable UUID toolVersionId,
            @Valid @RequestBody UpdateActivationToolVersionScanRequest request
    ) {
        return activationToolVersionService.updateScan(toolVersionId, request);
    }

    @PutMapping("/{toolVersionId}/manifest")
    public ActivationToolVersionResponse updateManifest(
            @PathVariable UUID toolVersionId,
            @Valid @RequestBody UpdateActivationToolVersionManifestRequest request
    ) {
        return activationToolVersionService.updateManifest(toolVersionId, request);
    }

    @DeleteMapping("/{toolVersionId}")
    public ResponseEntity<Void> delete(@PathVariable UUID toolVersionId) {
        activationToolVersionService.delete(toolVersionId);
        return ResponseEntity.noContent().build();
    }
}