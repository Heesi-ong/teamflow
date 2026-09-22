package com.teamflow.member;

import com.teamflow.activity.ActivityActionType;
import com.teamflow.activity.ProjectActivityEvent;
import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorCode;
import com.teamflow.member.dto.InvitationResponse;
import com.teamflow.member.dto.InviteRequest;
import com.teamflow.member.dto.ProjectMemberResponse;
import com.teamflow.notification.NotificationService;
import com.teamflow.notification.NotificationType;
import com.teamflow.project.ProjectRepository;
import com.teamflow.user.UserService;
import com.teamflow.user.UserSummary;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ProjectMember/Invitation 관리 및 프로젝트 단위 권한 검증, per
 * 03-functional-specification.md §3.6, 05-backend-architecture.md §3.
 */
@Service
public class ProjectMemberService {

    // 문서에 명시된 만료 기간이 없어 초대 링크의 통상적인 유효 기간인 7일로 정한다.
    private static final Duration INVITATION_EXPIRY = Duration.ofDays(7);

    private final ProjectMemberRepository projectMemberRepository;
    private final InvitationRepository invitationRepository;
    private final UserService userService;
    private final NotificationService notificationService;
    private final ProjectRepository projectRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final SecureRandom random = new SecureRandom();

    public ProjectMemberService(ProjectMemberRepository projectMemberRepository, InvitationRepository invitationRepository,
            UserService userService, NotificationService notificationService, ProjectRepository projectRepository,
            ApplicationEventPublisher eventPublisher) {
        this.projectMemberRepository = projectMemberRepository;
        this.invitationRepository = invitationRepository;
        this.userService = userService;
        this.notificationService = notificationService;
        this.projectRepository = projectRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ProjectMember addOwner(Long projectId, Long userId) {
        return projectMemberRepository.save(new ProjectMember(projectId, userId, ProjectRole.OWNER));
    }

    /** @return the requester's membership, once confirmed to be at least {@code minRole}. */
    public ProjectMember requireAtLeast(Long projectId, Long userId, ProjectRole minRole) {
        requireActiveProject(projectId);
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));
        if (!member.getRole().isAtLeast(minRole)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return member;
    }

    public boolean isMember(Long projectId, Long userId) {
        return projectRepository.existsByIdAndDeletedAtIsNull(projectId)
                && projectMemberRepository.existsByProjectIdAndUserId(projectId, userId);
    }

    /** Internal use (dashboard) — caller already verified membership. */
    public long countMembers(Long projectId) {
        return projectMemberRepository.findByProjectId(projectId).size();
    }

    public List<Long> findProjectIdsByUser(Long userId) {
        return projectMemberRepository.findByUserId(userId).stream().map(ProjectMember::getProjectId).toList();
    }

    @Transactional
    public Invitation invite(Long projectId, Long inviterId, InviteRequest request) {
        requireAtLeast(projectId, inviterId, ProjectRole.ADMIN);
        Long invitedUserId = null;
        if (request.email() != null) {
            invitedUserId = userService.findUserIdByEmail(request.email()).orElse(null);
            if (invitedUserId != null && projectMemberRepository.existsByProjectIdAndUserId(projectId, invitedUserId)) {
                throw new BusinessException(ErrorCode.ALREADY_MEMBER);
            }
        }
        ProjectRole role = request.role() != null ? request.role() : ProjectRole.MEMBER;
        if (role == ProjectRole.OWNER) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        Invitation invitation = new Invitation(projectId, request.email(), generateToken(), role, inviterId,
                OffsetDateTime.now().plus(INVITATION_EXPIRY));
        invitationRepository.save(invitation);
        eventPublisher.publishEvent(new ProjectActivityEvent(ActivityActionType.MEMBER_INVITED,
                projectId, inviterId, "팀원 초대가 생성됨"));
        // 이메일이 기존 가입 사용자와 일치할 때만 알림을 보낼 수 있다 — 링크 초대이거나
        // 아직 가입하지 않은 이메일이면 수락 시점까지 알림 대상이 존재하지 않는다.
        if (invitedUserId != null) {
            String inviterName = userService.getSummary(inviterId).name();
            notificationService.create(invitedUserId, NotificationType.PROJECT_INVITE,
                    inviterName + "님이 프로젝트에 초대했습니다.", "/invitations/" + invitation.getToken());
        }
        return invitation;
    }

    public List<InvitationResponse> listInvitations(Long projectId, Long requesterId, InvitationStatus status) {
        requireAtLeast(projectId, requesterId, ProjectRole.ADMIN);
        List<Invitation> invitations = status != null
                ? invitationRepository.findByProjectIdAndStatus(projectId, status)
                : invitationRepository.findByProjectId(projectId);
        return invitations.stream().map(InvitationResponse::from).toList();
    }

    @Transactional
    public void revokeInvitation(Long projectId, Long requesterId, Long invitationId) {
        requireAtLeast(projectId, requesterId, ProjectRole.ADMIN);
        Invitation invitation = invitationRepository.findById(invitationId)
                .filter(i -> i.getProjectId().equals(projectId))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITATION_NOT_FOUND));
        invitation.revoke();
    }

    @Transactional
    public ProjectMemberResponse acceptInvitation(String token, Long userId) {
        Invitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITATION_NOT_FOUND));
        if (!invitation.isUsable()) {
            throw new BusinessException(ErrorCode.INVITATION_EXPIRED);
        }
        requireActiveProject(invitation.getProjectId());
        UserSummary acceptingUser = userService.getSummary(userId);
        if (acceptingUser == null || (invitation.getEmail() != null
                && !invitation.getEmail().trim().equalsIgnoreCase(acceptingUser.email().trim()))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (projectMemberRepository.existsByProjectIdAndUserId(invitation.getProjectId(), userId)) {
            throw new BusinessException(ErrorCode.ALREADY_MEMBER);
        }
        ProjectMember member = projectMemberRepository.save(new ProjectMember(invitation.getProjectId(), userId, invitation.getRole()));
        invitation.accept();
        eventPublisher.publishEvent(new ProjectActivityEvent(ActivityActionType.MEMBER_JOINED,
                invitation.getProjectId(), userId, "팀원이 프로젝트에 참가함"));
        return ProjectMemberResponse.from(member, acceptingUser);
    }

    public List<ProjectMemberResponse> listMembers(Long projectId, Long requesterId) {
        requireAtLeast(projectId, requesterId, ProjectRole.GUEST);
        List<ProjectMember> members = projectMemberRepository.findByProjectId(projectId);
        Map<Long, UserSummary> users = userService.getSummaries(members.stream().map(ProjectMember::getUserId).toList());
        return members.stream().map(m -> ProjectMemberResponse.from(m, users.get(m.getUserId()))).toList();
    }

    @Transactional
    public ProjectMemberResponse changeRole(Long projectId, Long requesterId, Long memberId, ProjectRole newRole) {
        requireAtLeast(projectId, requesterId, ProjectRole.OWNER);
        if (newRole == ProjectRole.OWNER) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        ProjectMember member = getMemberInProject(projectId, memberId);
        if (member.getRole() == ProjectRole.OWNER) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        member.changeRole(newRole);
        eventPublisher.publishEvent(new ProjectActivityEvent(ActivityActionType.MEMBER_ROLE_CHANGED,
                projectId, requesterId, "팀원 역할이 " + newRole + "(으)로 변경됨"));
        return ProjectMemberResponse.from(member, userService.getSummary(member.getUserId()));
    }

    @Transactional
    public List<ProjectMemberResponse> transferOwnership(Long projectId, Long requesterId, Long memberId) {
        ProjectMember currentOwner = requireAtLeast(projectId, requesterId, ProjectRole.OWNER);
        ProjectMember newOwner = getMemberInProject(projectId, memberId);
        if (newOwner.getUserId().equals(requesterId)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        currentOwner.changeRole(ProjectRole.ADMIN);
        newOwner.changeRole(ProjectRole.OWNER);
        notificationService.create(newOwner.getUserId(), NotificationType.ANNOUNCEMENT,
                "프로젝트 소유권이 위임되었습니다.", "/projects/" + projectId);
        Map<Long, UserSummary> users = userService.getSummaries(List.of(currentOwner.getUserId(), newOwner.getUserId()));
        return List.of(
                ProjectMemberResponse.from(newOwner, users.get(newOwner.getUserId())),
                ProjectMemberResponse.from(currentOwner, users.get(currentOwner.getUserId())));
    }

    @Transactional
    public void removeMember(Long projectId, Long requesterId, Long memberId) {
        requireAtLeast(projectId, requesterId, ProjectRole.ADMIN);
        ProjectMember member = getMemberInProject(projectId, memberId);
        // 09-authentication-authorization.md §3.17과 동일한 불변식: 프로젝트는 항상 OWNER인
        // 멤버를 가져야 하므로, transfer-ownership 없이 OWNER를 제거할 수 없다.
        if (member.getRole() == ProjectRole.OWNER) {
            throw new BusinessException(ErrorCode.OWNER_CANNOT_LEAVE);
        }
        projectMemberRepository.delete(member);
        eventPublisher.publishEvent(new ProjectActivityEvent(ActivityActionType.MEMBER_REMOVED,
                projectId, requesterId, "팀원이 프로젝트에서 제거됨"));
    }

    @Transactional
    public void leave(Long projectId, Long userId) {
        requireActiveProject(projectId);
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));
        if (member.getRole() == ProjectRole.OWNER) {
            throw new BusinessException(ErrorCode.OWNER_CANNOT_LEAVE);
        }
        projectMemberRepository.delete(member);
        eventPublisher.publishEvent(new ProjectActivityEvent(ActivityActionType.MEMBER_LEFT,
                projectId, userId, "팀원이 프로젝트에서 탈퇴함"));
    }

    private ProjectMember getMemberInProject(Long projectId, Long memberId) {
        ProjectMember member = projectMemberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (!member.getProjectId().equals(projectId)) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
        return member;
    }

    private void requireActiveProject(Long projectId) {
        if (!projectRepository.existsByIdAndDeletedAtIsNull(projectId)) {
            throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND);
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
