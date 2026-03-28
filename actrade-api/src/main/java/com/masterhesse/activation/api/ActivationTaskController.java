package com.masterhesse.activation.api;

import com.masterhesse.activation.api.request.CreateActivationTaskRequest;
import com.masterhesse.activation.api.response.ActivationTaskResponse;
import com.masterhesse.activation.application.ActivationTaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/activation/tasks")
public class ActivationTaskController {

    private final ActivationTaskService activationTaskService;

    public ActivationTaskController(ActivationTaskService activationTaskService) {
        this.activationTaskService = activationTaskService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ActivationTaskResponse createTask(@Valid @RequestBody CreateActivationTaskRequest request) {
        return activationTaskService.createTask(request);
    }

    @GetMapping("/{id}")
    public ActivationTaskResponse getTaskById(@PathVariable Long id) {
        return activationTaskService.getTaskById(id);
    }
}