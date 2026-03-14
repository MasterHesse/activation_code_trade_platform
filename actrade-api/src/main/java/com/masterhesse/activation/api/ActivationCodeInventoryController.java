// src/main/java/com/masterhesse/activation/api/ActivationCodeInventoryController.java
package com.masterhesse.activation.api;

import com.masterhesse.activation.api.request.BatchCreateActivationCodeRequest;
import com.masterhesse.activation.api.request.CreateActivationCodeRequest;
import com.masterhesse.activation.api.request.UpdateActivationCodeStatusRequest;
import com.masterhesse.activation.api.response.ActivationCodeInventoryResponse;
import com.masterhesse.activation.application.ActivationCodeInventoryService;
import com.masterhesse.activation.domain.enums.ActivationCodeStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/activation-codes")
public class ActivationCodeInventoryController {

    private final ActivationCodeInventoryService activationCodeInventoryService;

    public ActivationCodeInventoryController(ActivationCodeInventoryService activationCodeInventoryService) {
        this.activationCodeInventoryService = activationCodeInventoryService;
    }

    @PostMapping
    public ResponseEntity<ActivationCodeInventoryResponse> create(
            @Valid @RequestBody CreateActivationCodeRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(activationCodeInventoryService.create(request));
    }

    @PostMapping("/batch")
    public ResponseEntity<List<ActivationCodeInventoryResponse>> batchCreate(
            @Valid @RequestBody BatchCreateActivationCodeRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(activationCodeInventoryService.batchCreate(request));
    }

    @GetMapping("/{codeId}")
    public ActivationCodeInventoryResponse getById(@PathVariable UUID codeId) {
        return activationCodeInventoryService.getById(codeId);
    }

    @GetMapping
    public Page<ActivationCodeInventoryResponse> page(
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) ActivationCodeStatus status,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return activationCodeInventoryService.page(productId, status, pageable);
    }

    @GetMapping("/batch/{batchNo}")
    public List<ActivationCodeInventoryResponse> findByBatchNo(@PathVariable String batchNo) {
        return activationCodeInventoryService.findByBatchNo(batchNo);
    }

    @PutMapping("/{codeId}/status")
    public ActivationCodeInventoryResponse updateStatus(
            @PathVariable UUID codeId,
            @Valid @RequestBody UpdateActivationCodeStatusRequest request
    ) {
        return activationCodeInventoryService.updateStatus(codeId, request);
    }

    @PutMapping("/{codeId}/void")
    public ActivationCodeInventoryResponse voidCode(@PathVariable UUID codeId) {
        return activationCodeInventoryService.voidCode(codeId);
    }

    @DeleteMapping("/{codeId}")
    public ResponseEntity<Void> delete(@PathVariable UUID codeId) {
        activationCodeInventoryService.delete(codeId);
        return ResponseEntity.noContent().build();
    }
}