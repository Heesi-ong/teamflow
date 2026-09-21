package com.teamflow.task;

import com.teamflow.auth.UserPrincipal;
import com.teamflow.common.dto.PageResponse;
import com.teamflow.task.dto.TaskAssigneeUpdateRequest;
import com.teamflow.task.dto.TaskCreateRequest;
import com.teamflow.task.dto.TaskDetailResponse;
import com.teamflow.task.dto.TaskResponse;
import com.teamflow.task.dto.TaskStatusUpdateRequest;
import com.teamflow.task.dto.TaskUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 08-api-specification.md §4 Task. */
@RestController
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/api/projects/{projectId}/tasks")
    public ResponseEntity<TaskResponse> create(
            @PathVariable Long projectId, @AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody TaskCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.create(projectId, principal.userId(), request));
    }

    @GetMapping("/api/projects/{projectId}/tasks")
    public PageResponse<TaskResponse> list(
            @PathVariable Long projectId, @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) TaskStatus status, @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) Long assigneeId, @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        return taskService.list(projectId, principal.userId(), status, priority, assigneeId, keyword, pageable);
    }

    @GetMapping("/api/projects/{projectId}/tasks/{taskId}")
    public TaskDetailResponse getDetail(
            @PathVariable Long projectId, @PathVariable Long taskId, @AuthenticationPrincipal UserPrincipal principal) {
        return taskService.getDetail(projectId, taskId, principal.userId());
    }

    @PatchMapping("/api/projects/{projectId}/tasks/{taskId}")
    public TaskResponse update(
            @PathVariable Long projectId, @PathVariable Long taskId, @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TaskUpdateRequest request) {
        return taskService.update(projectId, taskId, principal.userId(), request);
    }

    @PatchMapping("/api/projects/{projectId}/tasks/{taskId}/status")
    public TaskResponse changeStatus(
            @PathVariable Long projectId, @PathVariable Long taskId, @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TaskStatusUpdateRequest request) {
        return taskService.changeStatus(projectId, taskId, principal.userId(), request);
    }

    @PatchMapping("/api/projects/{projectId}/tasks/{taskId}/assignee")
    public TaskResponse changeAssignee(
            @PathVariable Long projectId, @PathVariable Long taskId, @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TaskAssigneeUpdateRequest request) {
        return taskService.changeAssignee(projectId, taskId, principal.userId(), request);
    }

    @DeleteMapping("/api/projects/{projectId}/tasks/{taskId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long projectId, @PathVariable Long taskId, @AuthenticationPrincipal UserPrincipal principal) {
        taskService.delete(projectId, taskId, principal.userId());
        return ResponseEntity.noContent().build();
    }
}
