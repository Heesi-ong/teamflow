package com.teamflow.project;

import com.teamflow.common.dto.PageResponse;
import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorCode;
import com.teamflow.member.ProjectMemberService;
import com.teamflow.member.ProjectRole;
import com.teamflow.member.dto.ProjectMemberResponse;
import com.teamflow.project.dto.ProjectCreateRequest;
import com.teamflow.project.dto.ProjectResponse;
import com.teamflow.project.dto.ProjectSummaryResponse;
import com.teamflow.project.dto.ProjectUpdateRequest;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 03-functional-specification.md §3.5, 08-api-specification.md §2. */
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberService projectMemberService;

    public ProjectService(ProjectRepository projectRepository, ProjectMemberService projectMemberService) {
        this.projectRepository = projectRepository;
        this.projectMemberService = projectMemberService;
    }

    @Transactional
    public ProjectResponse create(Long ownerId, ProjectCreateRequest request) {
        Project project = projectRepository.save(
                new Project(request.name(), request.description(), request.startDate(), request.endDate(), ownerId));
        projectMemberService.addOwner(project.getId(), ownerId);
        return ProjectResponse.from(project);
    }

    public PageResponse<ProjectSummaryResponse> listMine(Long userId, ProjectStatus status, Pageable pageable) {
        List<Long> projectIds = projectMemberService.findProjectIdsByUser(userId);
        if (projectIds.isEmpty()) {
            return new PageResponse<>(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0);
        }
        Page<Project> page = status != null
                ? projectRepository.findByIdInAndDeletedAtIsNullAndStatus(projectIds, status, pageable)
                : projectRepository.findByIdInAndDeletedAtIsNull(projectIds, pageable);
        return PageResponse.of(page.map(ProjectSummaryResponse::from));
    }

    public ProjectResponse get(Long projectId, Long userId) {
        Project project = findActive(projectId);
        projectMemberService.requireAtLeast(projectId, userId, ProjectRole.GUEST);
        return ProjectResponse.from(project);
    }

    @Transactional
    public ProjectResponse update(Long projectId, Long userId, ProjectUpdateRequest request) {
        Project project = findActive(projectId);
        projectMemberService.requireAtLeast(projectId, userId, ProjectRole.ADMIN);
        project.update(request.name(), request.description(), request.status(), request.startDate(), request.endDate());
        return ProjectResponse.from(project);
    }

    @Transactional
    public void delete(Long projectId, Long userId) {
        Project project = findActive(projectId);
        projectMemberService.requireAtLeast(projectId, userId, ProjectRole.OWNER);
        project.softDelete();
    }

    /** 03-functional-specification.md §3.17 step 4: owner_id must move with the OWNER role, in the same transaction. */
    @Transactional
    public List<ProjectMemberResponse> transferOwnership(Long projectId, Long requesterId, Long memberId) {
        Project project = findActive(projectId);
        List<ProjectMemberResponse> updated = projectMemberService.transferOwnership(projectId, requesterId, memberId);
        project.changeOwner(updated.get(0).userId());
        return updated;
    }

    private Project findActive(Long projectId) {
        return projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
    }
}
