package com.teamflow.task;

import com.teamflow.activity.TaskCreatedEvent;
import com.teamflow.activity.TaskDeletedEvent;
import com.teamflow.activity.TaskStatusChangedEvent;
import com.teamflow.activity.ActivityActionType;
import com.teamflow.activity.ProjectActivityEvent;
import com.teamflow.common.dto.PageResponse;
import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorCode;
import com.teamflow.member.ProjectMember;
import com.teamflow.member.ProjectMemberService;
import com.teamflow.member.ProjectRole;
import com.teamflow.notification.NotificationService;
import com.teamflow.notification.NotificationType;
import com.teamflow.task.dto.TaskAssigneeUpdateRequest;
import com.teamflow.task.dto.TaskChecklistResponse;
import com.teamflow.task.dto.TaskCreateRequest;
import com.teamflow.task.dto.TaskDetailResponse;
import com.teamflow.task.dto.TaskResponse;
import com.teamflow.task.dto.TaskStatusUpdateRequest;
import com.teamflow.task.dto.TaskUpdateRequest;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    public TaskService(TaskRepository taskRepository, TaskAssigneeRepository taskAssigneeRepository,
            TaskChecklistRepository taskChecklistRepository, ProjectMemberService projectMemberService,
            NotificationService notificationService, ApplicationEventPublisher eventPublisher) {
        this.taskRepository = taskRepository;
        this.taskAssigneeRepository = taskAssigneeRepository;
        this.taskChecklistRepository = taskChecklistRepository;
        this.projectMemberService = projectMemberService;
        this.notificationService = notificationService;
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
            if (!assigneeId.equals(authorId)) {
                notifyAssigned(task, assigneeId);
            }
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
        requireNonBlankIfPresent(request.title());
        validateDateRange(
                request.startDate() != null ? request.startDate() : task.getStartDate(),
                request.dueDate() != null ? request.dueDate() : task.getDueDate());
        task.updateInfo(request.title(), request.description(), request.priority(), request.startDate(), request.dueDate());
        eventPublisher.publishEvent(new ProjectActivityEvent(ActivityActionType.TASK_UPDATED,
                projectId, userId, "Task \"" + task.getTitle() + "\" 정보가 변경됨"));
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
        // 03-functional-specification.md §3.8: "담당자 외 관련자에게 Notification 생성" — 작성자에게 알린다.
        if (!task.getAuthorId().equals(userId)) {
            notificationService.create(task.getAuthorId(), NotificationType.TASK_STATUS_CHANGED,
                    "\"" + task.getTitle() + "\" 상태가 " + before + " → " + request.status() + "(으)로 변경되었습니다.",
                    taskTargetUrl(projectId, taskId));
        }
        return TaskResponse.from(task, currentAssignee(taskId));
    }

    @Transactional
    public TaskResponse changeAssignee(Long projectId, Long taskId, Long userId, TaskAssigneeUpdateRequest request) {
        Task task = findInProject(projectId, taskId);
        requireAuthorOrAdmin(projectId, taskId, userId, task);
        checkVersion(task, request.version());
        if (!request.clearAssignee() && !projectMemberService.isMember(projectId, request.assigneeId())) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
        taskAssigneeRepository.deleteByTaskId(taskId);
        if (!request.clearAssignee()) {
            taskAssigneeRepository.save(new TaskAssignee(taskId, request.assigneeId()));
        }
        // 담당자는 별도 테이블에 있지만 Task aggregate의 일부다. Task도 dirty 상태로 만들어
        // @Version을 증가시켜 동시 담당자 변경을 감지한다.
        task.markAssigneeChanged();
        taskRepository.flush();
        eventPublisher.publishEvent(new ProjectActivityEvent(ActivityActionType.TASK_ASSIGNEE_CHANGED,
                projectId, userId, "Task \"" + task.getTitle() + "\" 담당자가 변경됨"));
        if (!request.clearAssignee() && !request.assigneeId().equals(userId)) {
            notifyAssigned(task, request.assigneeId());
        }
        return TaskResponse.from(task, request.clearAssignee() ? null : request.assigneeId());
    }

    @Transactional
    public void delete(Long projectId, Long taskId, Long userId) {
        Task task = findInProject(projectId, taskId);
        requireAuthorOrAdmin(projectId, taskId, userId, task);
        taskRepository.delete(task);
        eventPublisher.publishEvent(new TaskDeletedEvent(projectId, taskId));
        eventPublisher.publishEvent(new ProjectActivityEvent(ActivityActionType.TASK_DELETED,
                projectId, userId, "Task \"" + task.getTitle() + "\" 삭제됨"));
    }

    /** Internal read for other modules' reporting needs (dashboard) — caller already verified membership. */
    public Map<TaskStatus, Long> countByStatus(Long projectId) {
        return Arrays.stream(TaskStatus.values())
                .collect(Collectors.toMap(status -> status, status -> taskRepository.countByProjectIdAndStatus(projectId, status)));
    }

    public long countAll(Long projectId) {
        return taskRepository.countByProjectId(projectId);
    }

    public List<TaskResponse> findDueSoon(Long projectId, int days) {
        LocalDate today = LocalDate.now();
        List<Task> tasks = taskRepository.findByProjectIdAndStatusNotAndDueDateBetweenOrderByDueDate(
                projectId, TaskStatus.DONE, today, today.plusDays(days));
        Map<Long, Long> assigneeByTask = currentAssignees(tasks.stream().map(Task::getId).toList());
        return tasks.stream().map(t -> TaskResponse.from(t, assigneeByTask.get(t.getId()))).toList();
    }

    public List<TaskResponse> search(Long projectId, String keyword, int limit) {
        Page<Task> page = taskRepository.search(projectId, null, null, null, keyword, PageRequest.of(0, limit));
        Map<Long, Long> assigneeByTask = currentAssignees(page.getContent().stream().map(Task::getId).toList());
        return page.getContent().stream().map(t -> TaskResponse.from(t, assigneeByTask.get(t.getId()))).toList();
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

    // title은 부분 업데이트라 null(=변경 안 함)은 허용하지만, 빈 문자열로 지우는 건 막는다.
    private void requireNonBlankIfPresent(String title) {
        if (title != null && title.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate dueDate) {
        if (startDate != null && dueDate != null && dueDate.isBefore(startDate)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
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

    private void notifyAssigned(Task task, Long assigneeId) {
        notificationService.create(assigneeId, NotificationType.TASK_ASSIGNED,
                "\"" + task.getTitle() + "\" Task의 담당자로 지정되었습니다.", taskTargetUrl(task.getProjectId(), task.getId()));
    }

    private String taskTargetUrl(Long projectId, Long taskId) {
        return "/projects/" + projectId + "/board?taskId=" + taskId;
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
