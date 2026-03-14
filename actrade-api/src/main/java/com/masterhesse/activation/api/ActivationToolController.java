// src/main/java/com/masterhesse/activation/api/ActivationToolController.java
package com.masterhesse.activation.api;

import com.masterhesse.activation.api.request.CreateActivationToolRequest;
import com.masterhesse.activation.api.request.SetCurrentVersionRequest;
import com.masterhesse.activation.api.request.UpdateActivationToolAuditStatusRequest;
import com.masterhesse.activation.api.request.UpdateActivationToolNameRequest;
import com.masterhesse.activation.api.request.UpdateActivationToolStatusRequest;
import com.masterhesse.activation.api.response.ActivationToolResponse;
import com.masterhesse.activation.application.ActivationToolService;
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
@RequestMapping("/api/admin/activation-tools")
public class ActivationToolController {

    private final ActivationToolService activationToolService;

    public ActivationToolController(ActivationToolService activationToolService) {
        this.activationToolService = activationToolService;
    }

    @PostMapping
    public ResponseEntity<ActivationToolResponse> create(@Valid @RequestBody CreateActivationToolRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(activationToolService.create(request));
    }

    @GetMapping("/{toolId}")
    public ActivationToolResponse getById(@PathVariable UUID toolId) {
        return activationToolService.getById(toolId);
    }

    @GetMapping
    public Page<ActivationToolResponse> page(
            @RequestParam(required = false) UUID merchantId,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return activationToolService.page(merchantId, pageable);
    }

    @PutMapping("/{toolId}/name")
    public ActivationToolResponse updateName(
            @PathVariable UUID toolId,
            @Valid @RequestBody UpdateActivationToolNameRequest request
    ) {
        return activationToolService.updateName(toolId, request);
    }

    @PutMapping("/{toolId}/status")
    public ActivationToolResponse updateStatus(
            @PathVariable UUID toolId,
            @Valid @RequestBody UpdateActivationToolStatusRequest request
    ) {
        return activationToolService.updateStatus(toolId, request);
    }

    @PutMapping("/{toolId}/audit-status")
    public ActivationToolResponse updateAuditStatus(
            @PathVariable UUID toolId,
            @Valid @RequestBody UpdateActivationToolAuditStatusRequest request
    ) {
        return activationToolService.updateAuditStatus(toolId, request);
    }

    @PutMapping("/{toolId}/current-version")
    public ActivationToolResponse setCurrentVersion(
            @PathVariable UUID toolId,
            @Valid @RequestBody SetCurrentVersionRequest request
    ) {
        return activationToolService.setCurrentVersion(toolId, request);
    }

    @DeleteMapping("/{toolId}")
    public ResponseEntity<Void> delete(@PathVariable UUID toolId) {
        activationToolService.delete(toolId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{toolId}/current-version")
    public ActivationToolResponse clearCurrentVersion(@PathVariable UUID toolId) {
        return activationToolService.clearCurrentVersion(toolId);
    }
}