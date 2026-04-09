package com.masterhesse.activation.api.internal.response;

public record FinishTaskResponse(
        boolean accepted,
        boolean duplicate,
        String taskStatus,
        boolean retryScheduled,
        Integer nextAttemptNo,
        String reason
) {

    public static FinishTaskResponse accepted(
            String taskStatus,
            boolean retryScheduled,
            Integer nextAttemptNo,
            String reason
    ) {
        return new FinishTaskResponse(
                true,
                false,
                taskStatus,
                retryScheduled,
                nextAttemptNo,
                reason
        );
    }

    public static FinishTaskResponse duplicate(
            String taskStatus,
            boolean retryScheduled,
            Integer nextAttemptNo,
            String reason
    ) {
        return new FinishTaskResponse(
                true,
                true,
                taskStatus,
                retryScheduled,
                nextAttemptNo,
                reason
        );
    }

    public static FinishTaskResponse rejected(
            String taskStatus,
            boolean retryScheduled,
            Integer nextAttemptNo,
            String reason
    ) {
        return new FinishTaskResponse(
                false,
                false,
                taskStatus,
                retryScheduled,
                nextAttemptNo,
                reason
        );
    }
}