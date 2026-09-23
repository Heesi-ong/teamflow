package com.teamflow.comment;

import com.teamflow.activity.ActivityActionType;
import com.teamflow.activity.ProjectActivityEvent;
import com.teamflow.comment.dto.TaskCommentCreateRequest;
import com.teamflow.comment.dto.TaskCommentResponse;
import com.teamflow.common.dto.PageResponse;
import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorCode;
import com.teamflow.member.ProjectMember;
import com.teamflow.member.ProjectMemberService;
import com.teamflow.member.ProjectRole;
import com.teamflow.member.dto.ProjectMemberResponse;
import com.teamflow.notification.NotificationService;
import com.teamflow.notification.NotificationType;
import com.teamflow.task.Task;
import com.teamflow.task.TaskRepository;
import com.teamflow.user.UserService;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 03-functional-specification.md §3.11, 08-api-specification.md §5. */
@Service
public class TaskCommentService {

    // 스키마에 별도 username 컬럼이 없어(07-database-design.md users 테이블), 화면에 표시되는
    // 프로젝트 멤버 이름(공백 없는 구간)을 @mention 대상으로 매칭한다.
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([\\w가-힣]+)");

    private final TaskCommentRepository taskCommentRepository;
    private final TaskRepository taskRepository;
    private final ProjectMemberService projectMemberService;
    private final NotificationService notificationService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    public TaskCommentService(TaskCommentRepository taskCommentRepository, TaskRepository taskRepository,
            ProjectMemberService projectMemberService, NotificationService notificationService, UserService userService,
            ApplicationEventPublisher eventPublisher) {
        this.taskCommentRepository = taskCommentRepository;
        this.taskRepository = taskRepository;
        this.projectMemberService = projectMemberService;
        this.notificationService = notificationService;
        this.userService = userService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public TaskCommentResponse create(Long taskId, Long authorId, TaskCommentCreateRequest request) {
        Task task = requireTask(taskId);
        // 09-authentication-authorization.md §7: 댓글 작성은 GUEST도 제한적으로 허용되므로
        // 최소 기준을 GUEST(=프로젝트 멤버 전체)로 둔다.
        projectMemberService.requireAtLeast(task.getProjectId(), authorId, ProjectRole.GUEST);
        TaskComment comment = taskCommentRepository.save(new TaskComment(taskId, authorId, request.content()));

        notifyMentions(task, authorId, request.content());
        eventPublisher.publishEvent(new ProjectActivityEvent(ActivityActionType.COMMENT_ADDED,
                task.getProjectId(), authorId, "Task \"" + task.getTitle() + "\"에 댓글이 작성됨"));

        String authorName = userService.getSummary(authorId).name();
        return TaskCommentResponse.from(comment, authorName);
    }

    public PageResponse<TaskCommentResponse> list(Long taskId, Long requesterId, Pageable pageable) {
        Task task = requireTask(taskId);
        projectMemberService.requireAtLeast(task.getProjectId(), requesterId, ProjectRole.GUEST);
        var page = taskCommentRepository.findByTaskId(taskId, pageable);
        var authorNames = userService.getSummaries(page.getContent().stream().map(TaskComment::getAuthorId).toList());
        return PageResponse.of(page.map(c -> TaskCommentResponse.from(c, authorNames.get(c.getAuthorId()).name())));
    }

    @Transactional
    public void delete(Long taskId, Long commentId, Long requesterId) {
        Task task = requireTask(taskId);
        TaskComment comment = taskCommentRepository.findByIdAndTaskId(commentId, taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
        ProjectMember member = projectMemberService.requireAtLeast(task.getProjectId(), requesterId, ProjectRole.GUEST);
        boolean isAuthor = comment.getAuthorId().equals(requesterId);
        if (!(isAuthor || member.getRole().isAtLeast(ProjectRole.ADMIN))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        taskCommentRepository.delete(comment);
    }

    /** Internal use (dashboard search) — caller already verified membership. */
    public List<TaskCommentResponse> search(Long projectId, String keyword, int limit) {
        List<TaskComment> comments = taskCommentRepository.search(projectId, keyword, PageRequest.of(0, limit));
        var authorNames = userService.getSummaries(comments.stream().map(TaskComment::getAuthorId).toList());
        return comments.stream().map(c -> TaskCommentResponse.from(c, authorNames.get(c.getAuthorId()).name())).toList();
    }

    private void notifyMentions(Task task, Long authorId, String content) {
        Matcher matcher = MENTION_PATTERN.matcher(content);
        if (!matcher.find()) {
            return;
        }
        List<ProjectMemberResponse> members = projectMemberService.listMembers(task.getProjectId(), authorId);
        String authorName = userService.getSummary(authorId).name();
        matcher.reset();
        while (matcher.find()) {
            String mentionedName = matcher.group(1);
            members.stream()
                    .filter(m -> m.userName().equalsIgnoreCase(mentionedName) && !m.userId().equals(authorId))
                    .findFirst()
                    // 존재하지 않는 Mention 대상은 무시한다 (03-functional-specification.md §3.11 예외 상황).
                    .ifPresent(m -> notificationService.create(m.userId(), NotificationType.MENTION,
                            authorName + "님이 댓글에서 회원님을 멘션했습니다.", "/projects/" + task.getProjectId() + "/board?taskId=" + task.getId()));
        }
    }

    private Task requireTask(Long taskId) {
        return taskRepository.findById(taskId).orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));
    }
}
