package com.teamflow.task.dto;

import jakarta.validation.constraints.NotBlank;

public record TaskChecklistCreateRequest(@NotBlank String content) {
}
