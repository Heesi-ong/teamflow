package com.teamflow.document.dto;

import com.teamflow.document.Document;
import java.time.OffsetDateTime;

public record DocumentResponse(
        Long id, Long projectId, Long authorId, String authorName, String title, String content,
        OffsetDateTime createdAt, OffsetDateTime updatedAt) {

    public static DocumentResponse from(Document document, String authorName) {
        return new DocumentResponse(document.getId(), document.getProjectId(), document.getAuthorId(), authorName,
                document.getTitle(), document.getContent(), document.getCreatedAt(), document.getUpdatedAt());
    }
}
