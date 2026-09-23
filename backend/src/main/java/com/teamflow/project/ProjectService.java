package com.teamflow.project;

import com.teamflow.activity.ActivityActionType;
import com.teamflow.activity.ProjectActivityEvent;
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
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

/** 03-functional-specification.md §3.5, 08-api-specification.md §2. */
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberService projectMemberService;
    private final ApplicationEventPublisher eventPublisher;

    public ProjectService(ProjectRepository projectRepository, ProjectMemberService projectMemberService,
            ApplicationEventPublisher eventPublisher) {
        this.projectRepository = projectRepository;
        this.projectMemberService = projectMemberService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ProjectResponse create(Long ownerId, ProjectCreateRequest request) {
        Project project = projectRepository.save(
                new Project(request.name(), request.description(), request.startDate(), request.endDate(), ownerId));
        projectMemberService.addOwner(project.getId(), ownerId);
        eventPublisher.publishEvent(new ProjectActivityEvent(ActivityActionType.PROJECT_CREATED,
                project.getId(), ownerId, "프로젝트가 생성됨"));
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
        // name은 부분 업데이트라 null(=변경 안 함)은 허용하지만, 빈 문자열로 지우는 건 막는다.
        if (request.getName() != null && request.getName().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        LocalDate nextStartDate = request.isClearStartDate()
                ? null
                : request.hasStartDate() ? request.getStartDate() : project.getStartDate();
        LocalDate nextEndDate = request.isClearEndDate()
                ? null
                : request.hasEndDate() ? request.getEndDate() : project.getEndDate();
        validateDateRange(nextStartDate, nextEndDate);
        project.update(request.getName(), request.getDescription(), request.getStatus(),
                nextStartDate, request.hasStartDate() || request.isClearStartDate(),
                nextEndDate, request.hasEndDate() || request.isClearEndDate());
        eventPublisher.publishEvent(new ProjectActivityEvent(ActivityActionType.PROJECT_UPDATED,
                projectId, userId, "프로젝트 정보가 변경됨"));
        return ProjectResponse.from(project);
    }

    @Transactional
    public void delete(Long projectId, Long userId) {
        Project project = findActive(projectId);
        projectMemberService.requireAtLeast(projectId, userId, ProjectRole.OWNER);
        project.softDelete();
        eventPublisher.publishEvent(new ProjectActivityEvent(ActivityActionType.PROJECT_DELETED,
                projectId, userId, "프로젝트가 삭제됨"));
    }

    /** 03-functional-specification.md §3.17 step 4: owner_id must move with the OWNER role, in the same transaction. */
    @Transactional
    public List<ProjectMemberResponse> transferOwnership(Long projectId, Long requesterId, Long memberId) {
        Project project = findActive(projectId);
        List<ProjectMemberResponse> updated = projectMemberService.transferOwnership(projectId, requesterId, memberId);
        // updated는 정확히 새 OWNER 1명 + 이전 OWNER(지금은 ADMIN) 1명이다. 리스트 순서(예: "0번이 새
        // 오너")에 기대는 대신 role로 찾는다 — 08-api-specification.md는 순서를 명시하지 않고, 순서
        // 의존은 ProjectMemberService 쪽 구현이 바뀌면 조용히 잘못된 owner_id를 심을 수 있다.
        Long newOwnerId = updated.stream()
                .filter(m -> m.role() == ProjectRole.OWNER)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("transferOwnership() did not return a member with role OWNER"))
                .userId();
        project.changeOwner(newOwnerId);
        eventPublisher.publishEvent(new ProjectActivityEvent(ActivityActionType.OWNERSHIP_TRANSFERRED,
                projectId, requesterId, "프로젝트 소유권이 위임됨"));
        return updated;
    }

    private Project findActive(Long projectId) {
        return projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
    }

    private void validateDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }
}
