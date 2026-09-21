package com.teamflow.task.dto;

import com.teamflow.task.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record TaskStatusUpdateRequest(@NotNull TaskStatus status, @NotNull Long version) {
}
