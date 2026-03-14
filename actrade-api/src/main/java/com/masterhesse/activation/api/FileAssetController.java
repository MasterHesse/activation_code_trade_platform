// src/main/java/com/masterhesse/activation/api/FileAssetController.java
package com.masterhesse.activation.api;

import com.masterhesse.activation.api.request.CreateFileAssetRequest;
import com.masterhesse.activation.api.request.UpdateFileAssetScanRequest;
import com.masterhesse.activation.api.response.FileAssetResponse;
import com.masterhesse.activation.application.FileAssetService;
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
@RequestMapping("/api/admin/file-assets")
public class FileAssetController {

    private final FileAssetService fileAssetService;

    public FileAssetController(FileAssetService fileAssetService) {
        this.fileAssetService = fileAssetService;
    }

    @PostMapping
    public ResponseEntity<FileAssetResponse> create(@Valid @RequestBody CreateFileAssetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fileAssetService.create(request));
    }

    @GetMapping("/{fileId}")
    public FileAssetResponse getById(@PathVariable UUID fileId) {
        return fileAssetService.getById(fileId);
    }

    @GetMapping
    public Page<FileAssetResponse> page(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return fileAssetService.page(pageable);
    }

    @GetMapping("/checksum/{checksumSha256}")
    public FileAssetResponse getByChecksum(@PathVariable String checksumSha256) {
        return fileAssetService.getByChecksum(checksumSha256);
    }

    @PutMapping("/{fileId}/scan")
    public FileAssetResponse updateScan(
            @PathVariable UUID fileId,
            @Valid @RequestBody UpdateFileAssetScanRequest request
    ) {
        return fileAssetService.updateScan(fileId, request);
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> delete(@PathVariable UUID fileId) {
        fileAssetService.delete(fileId);
        return ResponseEntity.noContent().build();
    }
}