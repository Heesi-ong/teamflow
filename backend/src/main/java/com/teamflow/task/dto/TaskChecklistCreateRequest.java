package com.teamflow.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TaskChecklistCreateRequest(@NotBlank @Size(max = 500) String content) {
}
