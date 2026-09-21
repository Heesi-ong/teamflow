package com.teamflow.task.dto;

import com.teamflow.task.Task;
import com.teamflow.task.TaskPriority;
import com.teamflow.task.TaskStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** Comment는 Phase 5(Comment/Notification)에서 추가된다 — 08-api-specification.md §4 "Checklist/Comment/Assignee 포함". */
public record TaskDetailResponse(
        Long id, Long projectId, String title, String description, Long authorId, Long assigneeId,
        TaskStatus status, TaskPriority priority, LocalDate startDate, LocalDate dueDate,
        Long version, OffsetDateTime createdAt, OffsetDateTime updatedAt, List<TaskChecklistResponse> checklists) {

    public static TaskDetailResponse from(Task task, Long assigneeId, List<TaskChecklistResponse> checklists) {
        return new TaskDetailResponse(task.getId(), task.getProjectId(), task.getTitle(), task.getDescription(), task.getAuthorId(),
                assigneeId, task.getStatus(), task.getPriority(), task.getStartDate(), task.getDueDate(),
                task.getVersion(), task.getCreatedAt(), task.getUpdatedAt(), checklists);
    }
}
