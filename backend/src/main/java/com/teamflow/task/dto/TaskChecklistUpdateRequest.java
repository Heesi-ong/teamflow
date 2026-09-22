package com.teamflow.task.dto;

import jakarta.validation.constraints.Size;

public record TaskChecklistUpdateRequest(@Size(max = 500) String content, Boolean isDone) {
}
