package com.teamflow.task.dto;

import com.teamflow.task.Task;
import com.teamflow.task.TaskPriority;
import com.teamflow.task.TaskStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record TaskResponse(
        Long id, Long projectId, String title, String description, Long authorId, Long assigneeId,
        TaskStatus status, TaskPriority priority, LocalDate startDate, LocalDate dueDate,
        Long version, OffsetDateTime createdAt, OffsetDateTime updatedAt) {

    public static TaskResponse from(Task task, Long assigneeId) {
        return new TaskResponse(task.getId(), task.getProjectId(), task.getTitle(), task.getDescription(), task.getAuthorId(),
                assigneeId, task.getStatus(), task.getPriority(), task.getStartDate(), task.getDueDate(),
                task.getVersion(), task.getCreatedAt(), task.getUpdatedAt());
    }
}
