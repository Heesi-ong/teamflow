package com.teamflow.task.dto;

import com.teamflow.task.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record TaskCreateRequest(
        @NotBlank String title, String description, Long assigneeId, TaskPriority priority, LocalDate startDate, LocalDate dueDate) {
}
