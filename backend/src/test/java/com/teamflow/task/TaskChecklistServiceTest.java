package com.teamflow.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorCode;
import com.teamflow.member.ProjectMember;
import com.teamflow.member.ProjectMemberService;
import com.teamflow.member.ProjectRole;
import com.teamflow.task.dto.TaskChecklistUpdateRequest;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 15-test-strategy.md §2 Unit Test. */
@ExtendWith(MockitoExtension.class)
class TaskChecklistServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private TaskChecklistRepository taskChecklistRepository;
    @Mock
    private ProjectMemberService projectMemberService;

    private TaskChecklistService newService() {
        return new TaskChecklistService(taskRepository, taskChecklistRepository, projectMemberService);
    }

    @Test
    void update_withBlankContent_throwsInvalidRequest() {
        // content는 체크박스만 토글할 때 null로 오는 부분 업데이트라 null은 허용하지만, 빈 문자열로
        // 지우는 시도는 막아야 한다.
        Task task = new Task(1L, "Title", null, 1L, TaskPriority.MEDIUM, null, null);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(projectMemberService.requireAtLeast(eq(1L), eq(1L), any())).thenReturn(new ProjectMember(1L, 1L, ProjectRole.OWNER));
        TaskChecklistService service = newService();

        assertThatThrownBy(() -> service.update(10L, 5L, 1L, new TaskChecklistUpdateRequest(" ", null)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    void update_toggleDoneOnly_keepsExistingContentUnchanged() {
        Task task = new Task(1L, "Title", null, 1L, TaskPriority.MEDIUM, null, null);
        TaskChecklist checklist = new TaskChecklist(10L, "Original", 0);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(projectMemberService.requireAtLeast(eq(1L), eq(1L), any())).thenReturn(new ProjectMember(1L, 1L, ProjectRole.OWNER));
        when(taskChecklistRepository.findById(5L)).thenReturn(Optional.of(checklist));
        TaskChecklistService service = newService();

        service.update(10L, 5L, 1L, new TaskChecklistUpdateRequest(null, true));

        assertThat(checklist.getContent()).isEqualTo("Original");
        assertThat(checklist.isDone()).isTrue();
    }
}
