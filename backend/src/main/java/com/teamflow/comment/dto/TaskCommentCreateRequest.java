package com.teamflow.comment.dto;

import jakarta.validation.constraints.NotBlank;

public record TaskCommentCreateRequest(@NotBlank String content) {
}
