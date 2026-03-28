package com.masterhesse.activation.domain.entity;

import com.masterhesse.activation.domain.enums.ActivationTaskArtifactType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "activation_task_artifact")
public class ActivationTaskArtifact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long taskId;

    private Long attemptId;

    @Enumerated(EnumType.STRING)
    private ActivationTaskArtifactType artifactType;

    private String fileName;

    private String filePath;

    private String mediaType;

    private Long fileSize;

    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Long attemptId) {
        this.attemptId = attemptId;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}