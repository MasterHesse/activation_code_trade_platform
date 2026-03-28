package com.masterhesse.activation.application.message;

public class ActivationTaskMessage {

    private Long taskId;
    private String taskNo;
    private Integer expectedAttemptNo;

    public ActivationTaskMessage() {
    }

    public ActivationTaskMessage(Long taskId, String taskNo, Integer expectedAttemptNo) {
        this.taskId = taskId;
        this.taskNo = taskNo;
        this.expectedAttemptNo = expectedAttemptNo;
    }

    public static ActivationTaskMessage of(Long taskId, String taskNo, Integer expectedAttemptNo) {
        return new ActivationTaskMessage(taskId, taskNo, expectedAttemptNo);
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getTaskNo() {
        return taskNo;
    }

    public void setTaskNo(String taskNo) {
        this.taskNo = taskNo;
    }

    public Integer getExpectedAttemptNo() {
        return expectedAttemptNo;
    }

    public void setExpectedAttemptNo(Integer expectedAttemptNo) {
        this.expectedAttemptNo = expectedAttemptNo;
    }
}