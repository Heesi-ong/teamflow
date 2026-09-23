package com.teamflow.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorCode;
import com.teamflow.notification.NotificationService;
import com.teamflow.project.ProjectRepository;
import com.teamflow.user.UserService;
import com.teamflow.user.UserSummary;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/** 15-test-strategy.md §2 Unit Test: 권한 검증 로직(requireAtLeast)과 소유권 관련 분기. */
@ExtendWith(MockitoExtension.class)
class ProjectMemberServiceTest {

    @Mock
    private ProjectMemberRepository projectMemberRepository;
    @Mock
    private InvitationRepository invitationRepository;
    @Mock
    private UserService userService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ProjectMemberService newService() {
        lenient().when(projectRepository.existsByIdAndDeletedAtIsNull(anyLong())).thenReturn(true);
        return new ProjectMemberService(projectMemberRepository, invitationRepository, userService, notificationService,
                projectRepository, eventPublisher);
    }

    @Test
    void requireAtLeast_deletedProject_throwsProjectNotFound() {
        ProjectMemberService service = newService();
        when(projectRepository.existsByIdAndDeletedAtIsNull(1L)).thenReturn(false);

        assertThatThrownBy(() -> service.requireAtLeast(1L, 1L, ProjectRole.GUEST))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PROJECT_NOT_FOUND);
    }

    @Test
    void requireAtLeast_nonMember_throwsForbidden() {
        when(projectMemberRepository.findByProjectIdAndUserId(1L, 99L)).thenReturn(Optional.empty());
        ProjectMemberService service = newService();

        assertThatThrownBy(() -> service.requireAtLeast(1L, 99L, ProjectRole.GUEST))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void requireAtLeast_belowRequiredRole_throwsForbidden() {
        // GUEST가 MEMBER 이상을 요구하는 작업(예: Task 생성)을 시도하는 경우.
        ProjectMember guest = new ProjectMember(1L, 2L, ProjectRole.GUEST);
        when(projectMemberRepository.findByProjectIdAndUserId(1L, 2L)).thenReturn(Optional.of(guest));
        ProjectMemberService service = newService();

        assertThatThrownBy(() -> service.requireAtLeast(1L, 2L, ProjectRole.MEMBER))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void requireAtLeast_sufficientRole_returnsMember() {
        ProjectMember admin = new ProjectMember(1L, 2L, ProjectRole.ADMIN);
        when(projectMemberRepository.findByProjectIdAndUserId(1L, 2L)).thenReturn(Optional.of(admin));
        ProjectMemberService service = newService();

        ProjectMember result = service.requireAtLeast(1L, 2L, ProjectRole.MEMBER);

        assertThat(result.getRole()).isEqualTo(ProjectRole.ADMIN);
    }

    @Test
    void leave_asOwner_throwsOwnerCannotLeave() {
        ProjectMember owner = new ProjectMember(1L, 1L, ProjectRole.OWNER);
        when(projectMemberRepository.findByProjectIdAndUserId(1L, 1L)).thenReturn(Optional.of(owner));
        ProjectMemberService service = newService();

        assertThatThrownBy(() -> service.leave(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.OWNER_CANNOT_LEAVE);
    }

    @Test
    void removeMember_targetIsOwner_throwsOwnerCannotLeave() {
        ProjectMember admin = new ProjectMember(1L, 2L, ProjectRole.ADMIN);
        ProjectMember owner = new ProjectMember(1L, 1L, ProjectRole.OWNER);
        when(projectMemberRepository.findByProjectIdAndUserId(1L, 2L)).thenReturn(Optional.of(admin));
        when(projectMemberRepository.findById(10L)).thenReturn(Optional.of(owner));
        ProjectMemberService service = newService();

        // ADMIN이 OWNER를 강제로 내보내려는 시도 — transfer-ownership 없이는 항상 거부되어야 한다.
        assertThatThrownBy(() -> service.removeMember(1L, 2L, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.OWNER_CANNOT_LEAVE);
    }

    @Test
    void transferOwnership_swapsRolesBetweenCurrentAndNewOwner() {
        ProjectMember currentOwner = new ProjectMember(1L, 1L, ProjectRole.OWNER);
        ProjectMember target = new ProjectMember(1L, 2L, ProjectRole.MEMBER);
        when(projectMemberRepository.findByProjectIdAndUserIdForUpdate(1L, 1L)).thenReturn(Optional.of(currentOwner));
        when(projectMemberRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(target));
        when(userService.getSummaries(any())).thenReturn(Map.of(
                1L, new UserSummary(1L, "owner@teamflow.dev", "Owner"),
                2L, new UserSummary(2L, "mate@teamflow.dev", "Mate")));
        ProjectMemberService service = newService();

        service.transferOwnership(1L, 1L, 20L);

        assertThat(currentOwner.getRole()).isEqualTo(ProjectRole.ADMIN);
        assertThat(target.getRole()).isEqualTo(ProjectRole.OWNER);
    }

    @Test
    void transferOwnership_toSelf_throwsInvalidRequest() {
        ProjectMember currentOwner = new ProjectMember(1L, 1L, ProjectRole.OWNER);
        when(projectMemberRepository.findByProjectIdAndUserIdForUpdate(1L, 1L)).thenReturn(Optional.of(currentOwner));
        when(projectMemberRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(currentOwner));

        assertThatThrownBy(() -> newService().transferOwnership(1L, 1L, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    void changeRole_targetIsCurrentOwner_throwsInvalidRequest() {
        ProjectMember currentOwner = new ProjectMember(1L, 1L, ProjectRole.OWNER);
        when(projectMemberRepository.findByProjectIdAndUserId(1L, 1L)).thenReturn(Optional.of(currentOwner));
        when(projectMemberRepository.findById(10L)).thenReturn(Optional.of(currentOwner));

        assertThatThrownBy(() -> newService().changeRole(1L, 1L, 10L, ProjectRole.ADMIN))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    void invite_withOwnerRole_throwsInvalidRequest() {
        ProjectMember admin = new ProjectMember(1L, 1L, ProjectRole.ADMIN);
        when(projectMemberRepository.findByProjectIdAndUserId(1L, 1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> newService().invite(1L, 1L,
                new com.teamflow.member.dto.InviteRequest(null, ProjectRole.OWNER)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    void acceptInvitation_emailDoesNotMatch_throwsForbidden() {
        Invitation invitation = new Invitation(1L, "invited@teamflow.dev", "token", ProjectRole.MEMBER, 1L,
                java.time.OffsetDateTime.now().plusDays(1));
        when(invitationRepository.findByToken("token")).thenReturn(Optional.of(invitation));
        when(userService.getSummary(2L)).thenReturn(new UserSummary(2L, "other@teamflow.dev", "Other"));

        assertThatThrownBy(() -> newService().acceptInvitation("token", 2L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }
}
