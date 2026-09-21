package com.teamflow.task.dto;

import jakarta.validation.constraints.NotNull;

public record TaskAssigneeUpdateRequest(@NotNull Long assigneeId, @NotNull Long version) {
}
