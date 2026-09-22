package com.teamflow.task;

import com.teamflow.auth.UserPrincipal;
import com.teamflow.task.dto.TaskChecklistCreateRequest;
import com.teamflow.task.dto.TaskChecklistResponse;
import com.teamflow.task.dto.TaskChecklistUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 08-api-specification.md §4 Checklist. */
@RestController
public class TaskChecklistController {

    private final TaskChecklistService taskChecklistService;

    public TaskChecklistController(TaskChecklistService taskChecklistService) {
        this.taskChecklistService = taskChecklistService;
    }

    @PostMapping("/api/tasks/{taskId}/checklists")
    public ResponseEntity<TaskChecklistResponse> add(
            @PathVariable Long taskId, @AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody TaskChecklistCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskChecklistService.add(taskId, principal.userId(), request));
    }

    @PatchMapping("/api/tasks/{taskId}/checklists/{checklistId}")
    public TaskChecklistResponse update(
            @PathVariable Long taskId, @PathVariable Long checklistId, @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TaskChecklistUpdateRequest request) {
        return taskChecklistService.update(taskId, checklistId, principal.userId(), request);
    }

    @DeleteMapping("/api/tasks/{taskId}/checklists/{checklistId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long taskId, @PathVariable Long checklistId, @AuthenticationPrincipal UserPrincipal principal) {
        taskChecklistService.delete(taskId, checklistId, principal.userId());
        return ResponseEntity.noContent().build();
    }
}
