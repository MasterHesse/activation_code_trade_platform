package com.masterhesse.activation.application;

import com.masterhesse.activation.application.message.ActivationTaskMessage;
import com.masterhesse.activation.config.RabbitMqConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ActivationTaskConsumer {

    private static final Logger log = LoggerFactory.getLogger(ActivationTaskConsumer.class);

    private final ActivationTaskRunner activationTaskRunner;

    public ActivationTaskConsumer(ActivationTaskRunner activationTaskRunner) {
        this.activationTaskRunner = activationTaskRunner;
    }

    @RabbitListener(queues = RabbitMqConfig.ACTIVATION_TASK_QUEUE)
    public void consume(ActivationTaskMessage message) {
        if (message == null || message.getTaskId() == null) {
            log.warn("Received empty activation task message");
            return;
        }

        log.info("Received activation task message. taskId={}, taskNo={}, expectedAttemptNo={}",
                message.getTaskId(), message.getTaskNo(), message.getExpectedAttemptNo());

        activationTaskRunner.run(message.getTaskId());
    }
}