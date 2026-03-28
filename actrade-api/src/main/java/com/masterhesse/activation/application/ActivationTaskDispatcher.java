package com.masterhesse.activation.application;

import com.masterhesse.activation.application.message.ActivationTaskMessage;
import com.masterhesse.activation.config.RabbitMqConfig;
import com.masterhesse.activation.domain.entity.ActivationTask;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class ActivationTaskDispatcher {

    private final RabbitTemplate rabbitTemplate;

    public ActivationTaskDispatcher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void dispatchNow(Long taskId, String taskNo) {
        ActivationTaskMessage message = ActivationTaskMessage.of(taskId, taskNo, null);
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.ACTIVATION_TASK_EXCHANGE,
                RabbitMqConfig.ACTIVATION_TASK_ROUTING_KEY,
                message
        );
    }

    public void dispatchAfterCommit(ActivationTask task) {
        if (task == null || task.getId() == null) {
            return;
        }

        Runnable action = () -> dispatchNow(task.getId(), task.getTaskNo());

        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            action.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    public void dispatchWithDelay(Long taskId, String taskNo, Integer expectedAttemptNo, long delayMillis) {
        ActivationTaskMessage message = ActivationTaskMessage.of(taskId, taskNo, expectedAttemptNo);

        rabbitTemplate.convertAndSend(
                RabbitMqConfig.ACTIVATION_TASK_RETRY_EXCHANGE,
                RabbitMqConfig.ACTIVATION_TASK_RETRY_ROUTING_KEY,
                message,
                rawMessage -> {
                    rawMessage.getMessageProperties().setExpiration(String.valueOf(Math.max(1000L, delayMillis)));
                    return rawMessage;
                }
        );
    }
}