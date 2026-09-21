package com.teamflow.comment.dto;

import com.teamflow.comment.TaskComment;
import java.time.OffsetDateTime;

public record TaskCommentResponse(
        Long id, Long taskId, Long authorId, String authorName, String content, OffsetDateTime createdAt, OffsetDateTime updatedAt) {

    public static TaskCommentResponse from(TaskComment comment, String authorName) {
        return new TaskCommentResponse(comment.getId(), comment.getTaskId(), comment.getAuthorId(), authorName,
                comment.getContent(), comment.getCreatedAt(), comment.getUpdatedAt());
    }
}
