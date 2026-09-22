package com.teamflow.project;

import com.teamflow.auth.UserPrincipal;
import com.teamflow.common.dto.PageResponse;
import com.teamflow.member.dto.ProjectMemberResponse;
import com.teamflow.project.dto.ProjectCreateRequest;
import com.teamflow.project.dto.ProjectResponse;
import com.teamflow.project.dto.ProjectSummaryResponse;
import com.teamflow.project.dto.ProjectUpdateRequest;
import jakarta.validation.Valid;
import java.util.List;
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

/** 08-api-specification.md §2 Project. */
@RestController
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping("/api/projects")
    public ResponseEntity<ProjectResponse> create(
            @AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody ProjectCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.create(principal.userId(), request));
    }

    @GetMapping("/api/projects")
    public PageResponse<ProjectSummaryResponse> list(
            @AuthenticationPrincipal UserPrincipal principal, @RequestParam(required = false) ProjectStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return projectService.listMine(principal.userId(), status, pageable);
    }

    @GetMapping("/api/projects/{projectId}")
    public ProjectResponse get(@PathVariable Long projectId, @AuthenticationPrincipal UserPrincipal principal) {
        return projectService.get(projectId, principal.userId());
    }

    @PatchMapping("/api/projects/{projectId}")
    public ProjectResponse update(
            @PathVariable Long projectId, @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ProjectUpdateRequest request) {
        return projectService.update(projectId, principal.userId(), request);
    }

    @DeleteMapping("/api/projects/{projectId}")
    public ResponseEntity<Void> delete(@PathVariable Long projectId, @AuthenticationPrincipal UserPrincipal principal) {
        projectService.delete(projectId, principal.userId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/projects/{projectId}/members/{memberId}/transfer-ownership")
    public List<ProjectMemberResponse> transferOwnership(
            @PathVariable Long projectId, @PathVariable Long memberId, @AuthenticationPrincipal UserPrincipal principal) {
        return projectService.transferOwnership(projectId, principal.userId(), memberId);
    }
}
