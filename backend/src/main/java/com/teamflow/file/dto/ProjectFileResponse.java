package com.teamflow.file.dto;

import com.teamflow.file.ProjectFile;
import java.time.OffsetDateTime;

public record ProjectFileResponse(
        Long id, Long projectId, Long taskId, Long uploaderId, String uploaderName, String fileName, long fileSize,
        String contentType, OffsetDateTime createdAt) {

    public static ProjectFileResponse from(ProjectFile file, String uploaderName) {
        return new ProjectFileResponse(file.getId(), file.getProjectId(), file.getTaskId(), file.getUploaderId(), uploaderName,
                file.getFileName(), file.getFileSize(), file.getContentType(), file.getCreatedAt());
    }
}
