package com.teamflow.task.dto;

import com.teamflow.task.TaskPriority;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record TaskUpdateRequest(
        String title, String description, TaskPriority priority, LocalDate startDate, LocalDate dueDate, @NotNull Long version) {
}
