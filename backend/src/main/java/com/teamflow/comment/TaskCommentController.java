package com.teamflow.comment;

import com.teamflow.auth.UserPrincipal;
import com.teamflow.comment.dto.TaskCommentCreateRequest;
import com.teamflow.comment.dto.TaskCommentResponse;
import com.teamflow.common.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 08-api-specification.md §5 Comment. */
@RestController
public class TaskCommentController {

    private final TaskCommentService taskCommentService;

    public TaskCommentController(TaskCommentService taskCommentService) {
        this.taskCommentService = taskCommentService;
    }

    @PostMapping("/api/tasks/{taskId}/comments")
    public ResponseEntity<TaskCommentResponse> create(
            @PathVariable Long taskId, @AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody TaskCommentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskCommentService.create(taskId, principal.userId(), request));
    }

    @GetMapping("/api/tasks/{taskId}/comments")
    public PageResponse<TaskCommentResponse> list(
            @PathVariable Long taskId, @AuthenticationPrincipal UserPrincipal principal, @PageableDefault(size = 20) Pageable pageable) {
        return taskCommentService.list(taskId, principal.userId(), pageable);
    }

    @DeleteMapping("/api/tasks/{taskId}/comments/{commentId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long taskId, @PathVariable Long commentId, @AuthenticationPrincipal UserPrincipal principal) {
        taskCommentService.delete(taskId, commentId, principal.userId());
        return ResponseEntity.noContent().build();
    }
}
