package com.teamflow.document.dto;

import com.teamflow.document.Document;
import java.time.OffsetDateTime;

public record DocumentSummaryResponse(
        Long id, Long projectId, Long authorId, String authorName, String title,
        OffsetDateTime createdAt, OffsetDateTime updatedAt) {

    public static DocumentSummaryResponse from(Document document, String authorName) {
        return new DocumentSummaryResponse(document.getId(), document.getProjectId(), document.getAuthorId(), authorName,
                document.getTitle(), document.getCreatedAt(), document.getUpdatedAt());
    }
}
