package com.teamflow.task.dto;

import com.teamflow.task.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TaskCreateRequest(
        @NotBlank @Size(max = 255) String title, String description, Long assigneeId, TaskPriority priority,
        LocalDate startDate, LocalDate dueDate) {

    @AssertTrue(message = "마감일은 시작일보다 빠를 수 없습니다.")
    public boolean isDateRangeValid() {
        return startDate == null || dueDate == null || !dueDate.isBefore(startDate);
    }
}
