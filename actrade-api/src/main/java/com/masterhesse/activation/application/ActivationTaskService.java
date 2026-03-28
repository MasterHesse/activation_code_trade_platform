package com.masterhesse.activation.application;

import com.masterhesse.activation.api.request.CreateActivationTaskRequest;
import com.masterhesse.activation.api.response.ActivationTaskResponse;
import com.masterhesse.activation.domain.entity.ActivationTask;
import com.masterhesse.activation.domain.enums.ActivationTaskSourceType;
import com.masterhesse.activation.domain.enums.ActivationTaskStatus;
import com.masterhesse.activation.persistence.ActivationTaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ActivationTaskService {

    private final ActivationTaskRepository activationTaskRepository;
    private final ActivationTaskDispatcher activationTaskDispatcher;

    public ActivationTaskService(ActivationTaskRepository activationTaskRepository,
                                 ActivationTaskDispatcher activationTaskDispatcher) {
        this.activationTaskRepository = activationTaskRepository;
        this.activationTaskDispatcher = activationTaskDispatcher;
    }

    @Transactional
    public ActivationTaskResponse createTask(CreateActivationTaskRequest request) {
        ActivationTask task = new ActivationTask();

        task.setTaskNo(generateTaskNo());
        task.setOrderId(request.getOrderId());
        task.setOrderItemId(request.getOrderItemId());
        task.setMerchantId(request.getMerchantId());
        task.setActivationToolId(request.getActivationToolId());
        task.setActivationToolVersionId(request.getActivationToolVersionId());
        task.setStatus(ActivationTaskStatus.PENDING);
        task.setSourceType(
                request.getSourceType() != null
                        ? request.getSourceType()
                        : ActivationTaskSourceType.ORDER_FULFILLMENT
        );
        task.setMaxAttempts(request.getMaxAttempts() != null ? request.getMaxAttempts() : 3);
        task.setAttemptCount(0);
        task.setPayloadJson(request.getPayloadJson());
        task.setScheduledAt(request.getScheduledAt() != null ? request.getScheduledAt() : LocalDateTime.now());

        ActivationTask saved = activationTaskRepository.save(task);

        // 数据库事务提交后再发 MQ，避免消费者先收到消息、但数据还未提交
        activationTaskDispatcher.dispatchAfterCommit(saved);

        return ActivationTaskResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ActivationTaskResponse getTaskById(Long id) {
        ActivationTask task = activationTaskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Activation task not found: " + id
                ));

        return ActivationTaskResponse.from(task);
    }

    private String generateTaskNo() {
        String timePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int randomPart = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "AT-" + timePart + "-" + randomPart;
    }
}