package com.teamflow.chat.dto;

import com.teamflow.chat.ChatMessage;
import java.time.OffsetDateTime;

public record ChatMessageResponse(
        Long id, Long projectId, Long authorId, String authorName, String content, OffsetDateTime createdAt) {

    public static ChatMessageResponse from(ChatMessage message, String authorName) {
        return new ChatMessageResponse(message.getId(), message.getProjectId(), message.getAuthorId(), authorName,
                message.getContent(), message.getCreatedAt());
    }
}
