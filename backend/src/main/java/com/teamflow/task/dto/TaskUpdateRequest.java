package com.teamflow.task.dto;

import com.teamflow.task.TaskPriority;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

// title/description/priority/startDate/dueDate는 부분 업데이트라 null 허용(TaskService에서 공백 문자열만 거른다).
public record TaskUpdateRequest(
        @Size(max = 255) String title, String description, TaskPriority priority, LocalDate startDate, LocalDate dueDate,
        @NotNull Long version) {
}
