package com.teamflow.comment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.teamflow.comment.dto.TaskCommentCreateRequest;
import com.teamflow.member.ProjectMember;
import com.teamflow.member.ProjectMemberService;
import com.teamflow.member.ProjectRole;
import com.teamflow.member.dto.ProjectMemberResponse;
import com.teamflow.notification.NotificationService;
import com.teamflow.notification.NotificationType;
import com.teamflow.task.Task;
import com.teamflow.task.TaskPriority;
import com.teamflow.task.TaskRepository;
import com.teamflow.user.UserService;
import com.teamflow.user.UserSummary;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/** 15-test-strategy.md §2 Unit Test: @mention 파서 로직(Utility 성격)을 create() 경유로 검증. */
@ExtendWith(MockitoExtension.class)
class TaskCommentServiceTest {

    private static final Long PROJECT_ID = 1L;
    private static final Long TASK_ID = 10L;
    private static final Long AUTHOR_ID = 1L;

    @Mock
    private TaskCommentRepository taskCommentRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private ProjectMemberService projectMemberService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private UserService userService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private TaskCommentService newService() {
        return new TaskCommentService(taskCommentRepository, taskRepository, projectMemberService, notificationService,
                userService, eventPublisher);
    }

    private void stubCommonFixtures() {
        Task task = new Task(PROJECT_ID, "Title", null, AUTHOR_ID, TaskPriority.MEDIUM, null, null);
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(projectMemberService.requireAtLeast(PROJECT_ID, AUTHOR_ID, ProjectRole.GUEST))
                .thenReturn(new ProjectMember(PROJECT_ID, AUTHOR_ID, ProjectRole.OWNER));
        when(taskCommentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userService.getSummary(AUTHOR_ID)).thenReturn(new UserSummary(AUTHOR_ID, "owner@teamflow.dev", "Owner"));
    }

    @Test
    void create_mentioningExistingMember_notifiesThatMember() {
        stubCommonFixtures();
        when(projectMemberService.listMembers(PROJECT_ID, AUTHOR_ID)).thenReturn(List.of(
                new ProjectMemberResponse(1L, AUTHOR_ID, "Owner", "owner@teamflow.dev", ProjectRole.OWNER, null),
                new ProjectMemberResponse(2L, 2L, "Mate", "mate@teamflow.dev", ProjectRole.MEMBER, null)));
        TaskCommentService service = newService();

        service.create(TASK_ID, AUTHOR_ID, new TaskCommentCreateRequest("@Mate please review"));

        verify(notificationService).create(eq(2L), eq(NotificationType.MENTION), anyString(), anyString());
    }

    @Test
    void create_mentioningUnknownName_isSilentlyIgnored() {
        // 03-functional-specification.md §3.11 예외 상황: 존재하지 않는 Mention 대상은 무시하고 댓글은 저장된다.
        stubCommonFixtures();
        when(projectMemberService.listMembers(PROJECT_ID, AUTHOR_ID)).thenReturn(List.of(
                new ProjectMemberResponse(1L, AUTHOR_ID, "Owner", "owner@teamflow.dev", ProjectRole.OWNER, null)));
        TaskCommentService service = newService();

        service.create(TASK_ID, AUTHOR_ID, new TaskCommentCreateRequest("@Nobody hello"));

        verify(notificationService, never()).create(any(), any(), anyString(), anyString());
    }

    @Test
    void create_selfMention_isNotNotified() {
        stubCommonFixtures();
        when(projectMemberService.listMembers(PROJECT_ID, AUTHOR_ID)).thenReturn(List.of(
                new ProjectMemberResponse(1L, AUTHOR_ID, "Owner", "owner@teamflow.dev", ProjectRole.OWNER, null)));
        TaskCommentService service = newService();

        service.create(TASK_ID, AUTHOR_ID, new TaskCommentCreateRequest("@Owner note to self"));

        verify(notificationService, never()).create(any(), any(), anyString(), anyString());
    }

    @Test
    void create_noMention_doesNotTouchMemberList() {
        stubCommonFixtures();
        TaskCommentService service = newService();

        service.create(TASK_ID, AUTHOR_ID, new TaskCommentCreateRequest("just a plain comment"));

        verify(projectMemberService, never()).listMembers(any(), any());
    }
}
