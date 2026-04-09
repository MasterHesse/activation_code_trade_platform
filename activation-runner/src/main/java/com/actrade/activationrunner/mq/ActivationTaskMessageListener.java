package com.actrade.activationrunner.mq;

import com.actrade.activationrunner.application.ActivationExecutionOrchestrator;
import com.actrade.activationrunner.metrics.RunnerMetrics;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(ActivationExecutionOrchestrator.class)
public class ActivationTaskMessageListener {

    private final ActivationExecutionOrchestrator activationExecutionOrchestrator;
    private final RunnerMetrics runnerMetrics;

    @RabbitListener(
            queues = "${runner.mq.queue}",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void onMessage(ActivationTaskDispatchMessage dispatchMessage,
                          Channel channel,
                          Message rawMessage) throws IOException {

        long deliveryTag = rawMessage.getMessageProperties().getDeliveryTag();
        Instant startTime = Instant.now();

        if (dispatchMessage == null || dispatchMessage.taskId() == null) {
            log.warn("Received invalid activation task message, reject directly. payload={}", rawMessage);
            channel.basicReject(deliveryTag, false);
            return;
        }

        log.info(
                "Received activation task message. taskId={}, taskNo={}, expectedAttemptNo={}, messageId={}, traceId={}",
                dispatchMessage.taskId(),
                dispatchMessage.taskNo(),
                dispatchMessage.expectedAttemptNo(),
                dispatchMessage.messageId(),
                dispatchMessage.traceId()
        );

        // 记录任务接收指标
        runnerMetrics.recordTaskReceived();

        MessageHandleResult result;
        try {
            // 记录任务开始
            runnerMetrics.recordTaskStarted();
            
            result = activationExecutionOrchestrator.handle(dispatchMessage);
            
            // 记录任务完成
            Duration executionTime = Duration.between(startTime, Instant.now());
            runnerMetrics.recordTaskCompleted(executionTime);
            
        } catch (Exception ex) {
            log.error(
                    "Activation task orchestration failed unexpectedly. taskId={}, taskNo={}, messageId={}",
                    dispatchMessage.taskId(),
                    dispatchMessage.taskNo(),
                    dispatchMessage.messageId(),
                    ex
            );
            
            // 记录任务失败
            Duration executionTime = Duration.between(startTime, Instant.now());
            runnerMetrics.recordTaskFailed("ORCHESTRATION_ERROR", executionTime);
            
            channel.basicNack(deliveryTag, false, true);
            return;
        }

        if (result == null || result.action() == null) {
            log.error(
                    "Activation orchestrator returned null result, requeue. taskId={}, taskNo={}, messageId={}",
                    dispatchMessage.taskId(),
                    dispatchMessage.taskNo(),
                    dispatchMessage.messageId()
            );
            
            runnerMetrics.recordTaskRetried();
            channel.basicNack(deliveryTag, false, true);
            return;
        }

        switch (result.action()) {
            case ACK -> {
                log.info(
                        "Activation task message acked. taskId={}, taskNo={}, reason={}, duration={}ms",
                        dispatchMessage.taskId(),
                        dispatchMessage.taskNo(),
                        result.reason(),
                        Duration.between(startTime, Instant.now()).toMillis()
                );
                channel.basicAck(deliveryTag, false);
            }
            case REQUEUE -> {
                log.warn(
                        "Activation task message requeued. taskId={}, taskNo={}, reason={}",
                        dispatchMessage.taskId(),
                        dispatchMessage.taskNo(),
                        result.reason()
                );
                runnerMetrics.recordTaskRetried();
                channel.basicNack(deliveryTag, false, true);
            }
            case REJECT -> {
                log.warn(
                        "Activation task message rejected. taskId={}, taskNo={}, reason={}",
                        dispatchMessage.taskId(),
                        dispatchMessage.taskNo(),
                        result.reason()
                );
                channel.basicReject(deliveryTag, false);
            }
        }
    }
}