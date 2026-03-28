package com.masterhesse.activation.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ActivationTaskExecutionResult {

    private boolean success;
    private String errorCode;
    private String errorMessage;
    private String summary;
    private Integer exitCode;
    private List<ActivationTaskExecutionArtifact> artifacts = new ArrayList<>();

    public ActivationTaskExecutionResult() {
    }

    public static ActivationTaskExecutionResult success(String summary,
                                                        Integer exitCode,
                                                        List<ActivationTaskExecutionArtifact> artifacts) {
        ActivationTaskExecutionResult result = new ActivationTaskExecutionResult();
        result.setSuccess(true);
        result.setSummary(summary);
        result.setExitCode(exitCode);
        result.setArtifacts(artifacts);
        return result;
    }

    public static ActivationTaskExecutionResult failed(String errorCode,
                                                       String errorMessage) {
        return failed(errorCode, errorMessage, null, Collections.emptyList());
    }

    public static ActivationTaskExecutionResult failed(String errorCode,
                                                       String errorMessage,
                                                       Integer exitCode,
                                                       List<ActivationTaskExecutionArtifact> artifacts) {
        ActivationTaskExecutionResult result = new ActivationTaskExecutionResult();
        result.setSuccess(false);
        result.setErrorCode(errorCode);
        result.setErrorMessage(errorMessage);
        result.setExitCode(exitCode);
        result.setArtifacts(artifacts);
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }


    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Integer getExitCode() {
        return exitCode;
    }

    public void setExitCode(Integer exitCode) {
        this.exitCode = exitCode;
    }

    public List<ActivationTaskExecutionArtifact> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(List<ActivationTaskExecutionArtifact> artifacts) {
        if (artifacts == null) {
            this.artifacts = new ArrayList<>();
            return;
        }
        this.artifacts = new ArrayList<>(artifacts);
    }
}