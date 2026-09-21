package com.teamflow.file;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "project_files")
public class ProjectFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "uploader_id", nullable = false)
    private Long uploaderId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "s3_key", nullable = false, unique = true)
    private String s3Key;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected ProjectFile() {
    }

    public ProjectFile(Long projectId, Long taskId, Long uploaderId, String fileName, String s3Key, long fileSize, String contentType) {
        this.projectId = projectId;
        this.taskId = taskId;
        this.uploaderId = uploaderId;
        this.fileName = fileName;
        this.s3Key = s3Key;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public Long getUploaderId() {
        return uploaderId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getS3Key() {
        return s3Key;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getContentType() {
        return contentType;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
