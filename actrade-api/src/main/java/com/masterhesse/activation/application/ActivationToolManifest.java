package com.masterhesse.activation.application;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActivationToolManifest {

    private List<String> entryCommand = new ArrayList<>();
    private List<String> arguments = new ArrayList<>();
    private String workingDirectory;
    private Map<String, String> env = new HashMap<>();
    private Integer timeoutSeconds;
    private List<Integer> successExitCodes = new ArrayList<>();
    private Boolean scanOutputDir;

    public List<String> getEntryCommand() {
        return entryCommand;
    }

    public void setEntryCommand(List<String> entryCommand) {
        this.entryCommand = entryCommand;
    }

    public List<String> getArguments() {
        return arguments;
    }

    public void setArguments(List<String> arguments) {
        this.arguments = arguments;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public void setEnv(Map<String, String> env) {
        this.env = env;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public List<Integer> getSuccessExitCodes() {
        return successExitCodes;
    }

    public void setSuccessExitCodes(List<Integer> successExitCodes) {
        this.successExitCodes = successExitCodes;
    }

    public Boolean getScanOutputDir() {
        return scanOutputDir;
    }

    public void setScanOutputDir(Boolean scanOutputDir) {
        this.scanOutputDir = scanOutputDir;
    }
}