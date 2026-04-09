package com.masterhesse.activation.api.internal;

import com.masterhesse.activation.api.internal.request.ClaimTaskRequest;
import com.masterhesse.activation.api.internal.request.FinishTaskRequest;
import com.masterhesse.activation.api.internal.response.ClaimTaskResponse;
import com.masterhesse.activation.api.internal.response.FinishTaskResponse;
import com.masterhesse.activation.application.InternalActivationTaskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/activation/tasks")
public class InternalActivationTaskController {

    private final InternalActivationTaskService internalActivationTaskService;

    public InternalActivationTaskController(InternalActivationTaskService internalActivationTaskService) {
        this.internalActivationTaskService = internalActivationTaskService;
    }

    @PostMapping("/claim")
    public ClaimTaskResponse claim(@Valid @RequestBody ClaimTaskRequest request) {
        return internalActivationTaskService.claim(request);
    }

    @PostMapping("/{taskId}/finish")
    public FinishTaskResponse finish(
            @PathVariable Long taskId,
            @Valid @RequestBody FinishTaskRequest request
    ) {
        return internalActivationTaskService.finish(taskId, request);
    }
}