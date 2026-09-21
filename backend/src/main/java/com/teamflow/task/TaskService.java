package com.teamflow.task;

import com.teamflow.activity.TaskCreatedEvent;
import com.teamflow.activity.TaskStatusChangedEvent;
import com.teamflow.common.dto.PageResponse;
import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorCode;
import com.teamflow.member.ProjectMember;
import com.teamflow.member.ProjectMemberService;
import com.teamflow.member.ProjectRole;
import com.teamflow.task.dto.TaskAssigneeUpdateRequest;
import com.teamflow.task.dto.TaskChecklistResponse;
import com.teamflow.task.dto.TaskCreateRequest;
import com.teamflow.task.dto.TaskDetailResponse;
import com.teamflow.task.dto.TaskResponse;
import com.teamflow.task.dto.TaskStatusUpdateRequest;
import com.teamflow.task.dto.TaskUpdateRequest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 03-functional-specification.md §3.7-3.9, 08-api-specification.md §4. */
@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskAssigneeRepository taskAssigneeRepository;
    private final TaskChecklistRepository taskChecklistRepository;
    private final ProjectMemberService projectMemberService;
    private final ApplicationEventPublisher eventPublisher;

    public TaskService(TaskRepository taskRepository, TaskAssigneeRepository taskAssigneeRepository,
            TaskChecklistRepository taskChecklistRepository, ProjectMemberService projectMemberService,
            ApplicationEventPublisher eventPublisher) {
        this.taskRepository = taskRepository;
        this.taskAssigneeRepository = taskAssigneeRepository;
        this.taskChecklistRepository = taskChecklistRepository;
        this.projectMemberService = projectMemberService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public TaskResponse create(Long projectId, Long authorId, TaskCreateRequest request) {
        projectMemberService.requireAtLeast(projectId, authorId, ProjectRole.MEMBER);
        if (request.assigneeId() != null && !projectMemberService.isMember(projectId, request.assigneeId())) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
        Task task = taskRepository.save(new Task(projectId, request.title(), request.description(), authorId,
                request.priority(), request.startDate(), request.dueDate()));
        Long assigneeId = null;
        if (request.assigneeId() != null) {
            taskAssigneeRepository.save(new TaskAssignee(task.getId(), request.assigneeId()));
            assigneeId = request.assigneeId();
        }
        eventPublisher.publishEvent(new TaskCreatedEvent(projectId, authorId, task.getId(), task.getTitle()));
        return TaskResponse.from(task, assigneeId);
    }

    public PageResponse<TaskResponse> list(Long projectId, Long userId, TaskStatus status, TaskPriority priority,
            Long assigneeId, String keyword, Pageable pageable) {
        projectMemberService.requireAtLeast(projectId, userId, ProjectRole.GUEST);
        Page<Task> page = taskRepository.search(projectId, status, priority, assigneeId, keyword, pageable);
        Map<Long, Long> assigneeByTask = currentAssignees(page.getContent().stream().map(Task::getId).toList());
        return PageResponse.of(page.map(task -> TaskResponse.from(task, assigneeByTask.get(task.getId()))));
    }

    public TaskDetailResponse getDetail(Long projectId, Long taskId, Long userId) {
        projectMemberService.requireAtLeast(projectId, userId, ProjectRole.GUEST);
        Task task = findInProject(projectId, taskId);
        List<TaskChecklistResponse> checklists = taskChecklistRepository.findByTaskIdOrderBySortOrder(taskId)
                .stream().map(TaskChecklistResponse::from).toList();
        return TaskDetailResponse.from(task, currentAssignee(taskId), checklists);
    }

    @Transactional
    public TaskResponse update(Long projectId, Long taskId, Long userId, TaskUpdateRequest request) {
        Task task = findInProject(projectId, taskId);
        requireAuthorAssigneeOrAdmin(projectId, taskId, userId, task);
        checkVersion(task, request.version());
        task.updateInfo(request.title(), request.description(), request.priority(), request.startDate(), request.dueDate());
        return TaskResponse.from(task, currentAssignee(taskId));
    }

    @Transactional
    public TaskResponse changeStatus(Long projectId, Long taskId, Long userId, TaskStatusUpdateRequest request) {
        Task task = findInProject(projectId, taskId);
        requireAuthorAssigneeOrAdmin(projectId, taskId, userId, task);
        checkVersion(task, request.version());
        TaskStatus before = task.getStatus();
        task.changeStatus(request.status());
        eventPublisher.publishEvent(
                new TaskStatusChangedEvent(projectId, userId, taskId, task.getTitle(), before.name(), request.status().name()));
        return TaskResponse.from(task, currentAssignee(taskId));
    }

    @Transactional
    public TaskResponse changeAssignee(Long projectId, Long taskId, Long userId, TaskAssigneeUpdateRequest request) {
        Task task = findInProject(projectId, taskId);
        requireAuthorOrAdmin(projectId, taskId, userId, task);
        checkVersion(task, request.version());
        if (!projectMemberService.isMember(projectId, request.assigneeId())) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
        taskAssigneeRepository.deleteByTaskId(taskId);
        taskAssigneeRepository.save(new TaskAssignee(taskId, request.assigneeId()));
        return TaskResponse.from(task, request.assigneeId());
    }

    @Transactional
    public void delete(Long projectId, Long taskId, Long userId) {
        Task task = findInProject(projectId, taskId);
        requireAuthorOrAdmin(projectId, taskId, userId, task);
        taskRepository.delete(task);
    }

    private Task findInProject(Long projectId, Long taskId) {
        return taskRepository.findByIdAndProjectId(taskId, projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));
    }

    private void checkVersion(Task task, Long expectedVersion) {
        if (!task.getVersion().equals(expectedVersion)) {
            throw new BusinessException(ErrorCode.TASK_VERSION_CONFLICT);
        }
    }

    private void requireAuthorAssigneeOrAdmin(Long projectId, Long taskId, Long userId, Task task) {
        ProjectMember member = projectMemberService.requireAtLeast(projectId, userId, ProjectRole.GUEST);
        boolean isAuthor = task.getAuthorId().equals(userId);
        boolean isAssignee = taskAssigneeRepository.existsByTaskIdAndUserId(taskId, userId);
        if (!(isAuthor || isAssignee || member.getRole().isAtLeast(ProjectRole.ADMIN))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void requireAuthorOrAdmin(Long projectId, Long taskId, Long userId, Task task) {
        ProjectMember member = projectMemberService.requireAtLeast(projectId, userId, ProjectRole.GUEST);
        boolean isAuthor = task.getAuthorId().equals(userId);
        if (!(isAuthor || member.getRole().isAtLeast(ProjectRole.ADMIN))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private Long currentAssignee(Long taskId) {
        return taskAssigneeRepository.findByTaskId(taskId).stream().findFirst().map(TaskAssignee::getUserId).orElse(null);
    }

    private Map<Long, Long> currentAssignees(List<Long> taskIds) {
        if (taskIds.isEmpty()) {
            return Map.of();
        }
        return taskAssigneeRepository.findByTaskIdIn(taskIds).stream()
                .collect(Collectors.toMap(TaskAssignee::getTaskId, TaskAssignee::getUserId, (a, b) -> a));
    }
}
