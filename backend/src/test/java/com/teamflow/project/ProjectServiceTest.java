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
import com.teamflow.member.dto.ProjectMemberResponse;
import com.teamflow.project.dto.ProjectUpdateRequest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/** 15-test-strategy.md §2 Unit Test. */
@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectMemberService projectMemberService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ProjectService newService() {
        return new ProjectService(projectRepository, projectMemberService, eventPublisher);
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

    @Test
    void update_withResolvedInvalidDateRange_throwsInvalidRequest() {
        Project project = new Project("Original", null, java.time.LocalDate.of(2026, 10, 10),
                java.time.LocalDate.of(2026, 10, 20), 1L);
        when(projectRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(project));
        when(projectMemberService.requireAtLeast(eq(1L), eq(1L), any()))
                .thenReturn(new ProjectMember(1L, 1L, ProjectRole.OWNER));

        assertThatThrownBy(() -> newService().update(1L, 1L,
                new ProjectUpdateRequest(null, null, null, java.time.LocalDate.of(2026, 10, 25), null)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    void transferOwnership_picksNewOwnerByRole_notByListPosition() {
        // ProjectMemberService.transferOwnership()이 반환하는 리스트에서 "0번째가 새 오너"라는 순서에
        // 기대면 안 된다 — 일부러 이전 오너(now ADMIN)를 0번째에, 새 오너를 1번째에 둬서 순서를 뒤집는다.
        Project project = new Project("Team", null, null, null, 1L);
        when(projectRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(project));
        ProjectMemberResponse previousOwnerNowAdmin =
                new ProjectMemberResponse(100L, 1L, "Owner", "owner@teamflow.dev", ProjectRole.ADMIN, null);
        ProjectMemberResponse newOwner =
                new ProjectMemberResponse(200L, 2L, "Mate", "mate@teamflow.dev", ProjectRole.OWNER, null);
        when(projectMemberService.transferOwnership(1L, 1L, 200L))
                .thenReturn(List.of(previousOwnerNowAdmin, newOwner));
        ProjectService service = newService();

        service.transferOwnership(1L, 1L, 200L);

        assertThat(project.getOwnerId()).isEqualTo(2L);
    }
}
