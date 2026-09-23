package com.teamflow.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.teamflow.activity.ActivityActionType;
import com.teamflow.activity.ProjectActivityEvent;
import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorCode;
import com.teamflow.member.ProjectMember;
import com.teamflow.member.ProjectMemberService;
import com.teamflow.member.ProjectRole;
import com.teamflow.notification.NotificationService;
import com.teamflow.task.dto.TaskCreateRequest;
import com.teamflow.task.dto.TaskAssigneeUpdateRequest;
import com.teamflow.task.dto.TaskStatusUpdateRequest;
import com.teamflow.task.dto.TaskUpdateRequest;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

/** 15-test-strategy.md §2 Unit Test. */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private TaskAssigneeRepository taskAssigneeRepository;
    @Mock
    private TaskChecklistRepository taskChecklistRepository;
    @Mock
    private ProjectMemberService projectMemberService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private TaskService newService() {
        return new TaskService(taskRepository, taskAssigneeRepository, taskChecklistRepository, projectMemberService,
                notificationService, eventPublisher);
    }

    /** @Version 필드는 setter가 없고, 영속화되지 않은 순수 new Task(...)는 null이라 실제 DB 로드 상태(0)를 흉내낸다. */
    private static Task taskFixture(Long projectId, Long authorId) {
        Task task = new Task(projectId, "Title", null, authorId, TaskPriority.MEDIUM, null, null);
        ReflectionTestUtils.setField(task, "version", 0L);
        return task;
    }

    @Test
    void create_requesterBelowMember_throwsForbidden() {
        // 15-test-strategy.md §2 예시: "GUEST가 Task 생성을 시도하면 Forbidden이 발생한다".
        when(projectMemberService.requireAtLeast(1L, 2L, ProjectRole.MEMBER))
                .thenThrow(new BusinessException(ErrorCode.FORBIDDEN));
        TaskService service = newService();

        assertThatThrownBy(() -> service.create(1L, 2L, new TaskCreateRequest("Title", null, null, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void create_assigneeNotProjectMember_throwsMemberNotFound() {
        when(projectMemberService.requireAtLeast(1L, 1L, ProjectRole.MEMBER))
                .thenReturn(new ProjectMember(1L, 1L, ProjectRole.OWNER));
        when(projectMemberService.isMember(1L, 999L)).thenReturn(false);
        TaskService service = newService();

        assertThatThrownBy(() -> service.create(1L, 1L, new TaskCreateRequest("Title", null, 999L, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    void changeStatus_doneBackToTodo_publishesEventWithCorrectBeforeAndAfter() {
        // 15-test-strategy.md §2 예시: "Task 상태를 DONE → TODO로 되돌릴 때 ActivityLog 메시지가 올바르게 생성된다"
        // (ActivityLog 자체는 activity 모듈이 이 이벤트를 구독해 기록하므로, 여기서는 발행되는 이벤트 내용을 검증한다).
        Task task = taskFixture(1L, 1L);
        task.changeStatus(TaskStatus.DONE);
        when(taskRepository.findByIdAndProjectId(10L, 1L)).thenReturn(Optional.of(task));
        when(projectMemberService.requireAtLeast(1L, 1L, ProjectRole.GUEST)).thenReturn(new ProjectMember(1L, 1L, ProjectRole.OWNER));
        when(taskAssigneeRepository.existsByTaskIdAndUserId(any(), any())).thenReturn(false);
        when(taskAssigneeRepository.findByTaskId(any())).thenReturn(java.util.List.of());
        TaskService service = newService();

        service.changeStatus(1L, 10L, 1L, new TaskStatusUpdateRequest(TaskStatus.TODO, 0L));

        verify(eventPublisher).publishEvent(eq(new ProjectActivityEvent(
                ActivityActionType.TASK_STATUS_CHANGED, 1L, 1L, "Task \"Title\" 상태 변경: DONE → TODO")));
    }

    @Test
    void changeStatus_staleVersion_throwsVersionConflict() {
        Task task = taskFixture(1L, 1L);
        when(taskRepository.findByIdAndProjectId(10L, 1L)).thenReturn(Optional.of(task));
        when(projectMemberService.requireAtLeast(1L, 1L, ProjectRole.GUEST)).thenReturn(new ProjectMember(1L, 1L, ProjectRole.OWNER));
        when(taskAssigneeRepository.existsByTaskIdAndUserId(any(), any())).thenReturn(false);
        TaskService service = newService();

        // 엔티티의 실제 version은 0인데 클라이언트가 오래된 값(99)을 보냈다고 가정.
        assertThatThrownBy(() -> service.changeStatus(1L, 10L, 1L, new TaskStatusUpdateRequest(TaskStatus.DONE, 99L)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.TASK_VERSION_CONFLICT);
    }

    @Test
    void update_withBlankTitle_throwsInvalidRequest() {
        // title은 부분 업데이트라 null(=변경 안 함)은 허용하지만, 빈 문자열로 지우는 시도는 막아야 한다.
        Task task = taskFixture(1L, 1L);
        when(taskRepository.findByIdAndProjectId(10L, 1L)).thenReturn(Optional.of(task));
        when(projectMemberService.requireAtLeast(1L, 1L, ProjectRole.GUEST)).thenReturn(new ProjectMember(1L, 1L, ProjectRole.OWNER));
        when(taskAssigneeRepository.existsByTaskIdAndUserId(any(), any())).thenReturn(false);
        TaskService service = newService();

        assertThatThrownBy(() -> service.update(1L, 10L, 1L, new TaskUpdateRequest("  ", null, null, null, null, 0L)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    void update_withNullTitle_keepsExistingTitleUnchanged() {
        // title 없이 description만 바꾸는 부분 업데이트는 계속 허용돼야 한다.
        Task task = taskFixture(1L, 1L);
        when(taskRepository.findByIdAndProjectId(10L, 1L)).thenReturn(Optional.of(task));
        when(projectMemberService.requireAtLeast(1L, 1L, ProjectRole.GUEST)).thenReturn(new ProjectMember(1L, 1L, ProjectRole.OWNER));
        when(taskAssigneeRepository.existsByTaskIdAndUserId(any(), any())).thenReturn(false);
        when(taskAssigneeRepository.findByTaskId(any())).thenReturn(java.util.List.of());
        TaskService service = newService();

        service.update(1L, 10L, 1L, new TaskUpdateRequest(null, "New description", null, null, null, 0L));

        assertThat(task.getTitle()).isEqualTo("Title");
        assertThat(task.getDescription()).isEqualTo("New description");
    }

    @Test
    void changeStatus_strangerNeitherAuthorAssigneeNorAdmin_throwsForbidden() {
        Task task = taskFixture(1L, 1L);
        when(taskRepository.findByIdAndProjectId(10L, 1L)).thenReturn(Optional.of(task));
        when(projectMemberService.requireAtLeast(1L, 3L, ProjectRole.GUEST)).thenReturn(new ProjectMember(1L, 3L, ProjectRole.MEMBER));
        when(taskAssigneeRepository.existsByTaskIdAndUserId(10L, 3L)).thenReturn(false);
        TaskService service = newService();

        assertThat(task.getAuthorId()).isNotEqualTo(3L);
        assertThatThrownBy(() -> service.changeStatus(1L, 10L, 3L, new TaskStatusUpdateRequest(TaskStatus.DONE, 0L)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void changeAssignee_marksTaskDirtyAndFlushesVersion() {
        Task task = taskFixture(1L, 1L);
        when(taskRepository.findByIdAndProjectId(10L, 1L)).thenReturn(Optional.of(task));
        when(projectMemberService.requireAtLeast(1L, 1L, ProjectRole.GUEST))
                .thenReturn(new ProjectMember(1L, 1L, ProjectRole.OWNER));
        when(projectMemberService.isMember(1L, 2L)).thenReturn(true);

        newService().changeAssignee(1L, 10L, 1L, new TaskAssigneeUpdateRequest(2L, 0L));

        assertThat(task.getUpdatedAt()).isNotNull();
        verify(taskRepository).flush();
    }

    @Test
    void changeAssignee_clearAssignmentDeletesCurrentAssignee() {
        Task task = taskFixture(1L, 1L);
        when(taskRepository.findByIdAndProjectId(10L, 1L)).thenReturn(Optional.of(task));
        when(projectMemberService.requireAtLeast(1L, 1L, ProjectRole.GUEST))
                .thenReturn(new ProjectMember(1L, 1L, ProjectRole.OWNER));

        newService().changeAssignee(1L, 10L, 1L, new TaskAssigneeUpdateRequest(null, 0L, true));

        verify(taskAssigneeRepository).deleteByTaskId(10L);
        verify(taskRepository).flush();
    }
}
