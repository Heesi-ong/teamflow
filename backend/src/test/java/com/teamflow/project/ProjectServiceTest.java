package com.teamflow.project;

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
import com.teamflow.project.dto.ProjectUpdateRequest;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 15-test-strategy.md §2 Unit Test. */
@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectMemberService projectMemberService;

    private ProjectService newService() {
        return new ProjectService(projectRepository, projectMemberService);
    }

    @Test
    void update_withBlankName_throwsInvalidRequest() {
        // name은 부분 업데이트라 null(=변경 안 함)은 허용하지만, 빈 문자열로 지우는 시도는 막아야 한다.
        Project project = new Project("Original", null, null, null, 1L);
        when(projectRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(project));
        when(projectMemberService.requireAtLeast(eq(1L), eq(1L), any())).thenReturn(new ProjectMember(1L, 1L, ProjectRole.OWNER));
        ProjectService service = newService();

        assertThatThrownBy(() -> service.update(1L, 1L, new ProjectUpdateRequest(" ", null, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    void update_withNullName_keepsExistingNameUnchanged() {
        Project project = new Project("Original", null, null, null, 1L);
        when(projectRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(project));
        when(projectMemberService.requireAtLeast(eq(1L), eq(1L), any())).thenReturn(new ProjectMember(1L, 1L, ProjectRole.OWNER));
        ProjectService service = newService();

        service.update(1L, 1L, new ProjectUpdateRequest(null, "New description", null, null, null));

        assertThat(project.getName()).isEqualTo("Original");
    }
}
