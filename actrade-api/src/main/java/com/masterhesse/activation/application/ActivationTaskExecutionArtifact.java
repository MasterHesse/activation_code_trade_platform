package com.masterhesse.activation.application;

import com.masterhesse.activation.domain.enums.ActivationTaskArtifactType;

public class ActivationTaskExecutionArtifact {

    private ActivationTaskArtifactType artifactType;
    private String fileName;
    private String filePath;
    private String mediaType;
    private Long fileSize;

    public ActivationTaskExecutionArtifact() {
    }

    public ActivationTaskExecutionArtifact(ActivationTaskArtifactType artifactType,
                                           String fileName,
                                           String filePath,
                                           String mediaType,
                                           Long fileSize) {
        this.artifactType = artifactType;
        this.fileName = fileName;
        this.filePath = filePath;
        this.mediaType = mediaType;
        this.fileSize = fileSize;
    }

    public ActivationTaskArtifactType getArtifactType() {
        return artifactType;
    }

    public void setArtifactType(ActivationTaskArtifactType artifactType) {
        this.artifactType = artifactType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
}