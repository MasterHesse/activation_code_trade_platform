package com.actrade.activationrunner.client;

import com.actrade.activationrunner.client.dto.ClaimTaskRequest;
import com.actrade.activationrunner.client.dto.ClaimTaskResponse;
import com.actrade.activationrunner.client.dto.FinishTaskRequest;
import com.actrade.activationrunner.client.dto.FinishTaskResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActradeInternalApiClient {

    /**
     * 如你的 actrade-api 内部接口路径不同，改这里即可。
     */
    private static final String CLAIM_TASK_PATH = "/internal/activation/tasks/claim";
    private static final String FINISH_TASK_PATH = "/internal/activation/tasks/{taskId}/finish";

    private final RestClient actradeInternalRestClient;

    public ClaimTaskResponse claimTask(ClaimTaskRequest request) {
        try {
            ClaimTaskResponse response = actradeInternalRestClient.post()
                    .uri(CLAIM_TASK_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ClaimTaskResponse.class);

            if (response == null) {
                throw new IllegalStateException("Claim task response is null");
            }

            log.info(
                    "Claim task completed. taskNo={}, claimed={}, reason={}, attemptNo={}",
                    request.taskNo(),
                    response.claimed(),
                    response.reason(),
                    response.attemptNo()
            );

            return response;
        } catch (RestClientException ex) {
            log.error(
                    "Claim task request failed. taskNo={}, runnerInstanceId={}, expectedAttemptNo={}, messageId={}",
                    request.taskNo(),
                    request.runnerInstanceId(),
                    request.expectedAttemptNo(),
                    request.messageId(),
                    ex
            );
            throw ex;
        }
    }

    public FinishTaskResponse finishTask(Long taskId, FinishTaskRequest request) {
        try {
            FinishTaskResponse response = actradeInternalRestClient.post()
                    .uri(FINISH_TASK_PATH, taskId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(FinishTaskResponse.class);

            if (response == null) {
                throw new IllegalStateException("Finish task response is null");
            }

            log.info(
                    "Finish task completed. taskId={}, success={}, accepted={}, duplicate={}, taskStatus={}, retryScheduled={}, nextAttemptNo={}",
                    taskId,
                    request.success(),
                    response.accepted(),
                    response.duplicate(),
                    response.taskStatus(),
                    response.retryScheduled(),
                    response.nextAttemptNo()
            );

            return response;
        } catch (RestClientException ex) {
            log.error(
                    "Finish task request failed. taskId={}, success={}, exitCode={}, timedOut={}",
                    taskId,
                    request.success(),
                    request.exitCode(),
                    request.timedOut(),
                    ex
            );
            throw ex;
        }
    }
}