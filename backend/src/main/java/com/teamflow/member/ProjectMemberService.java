package com.teamflow.member;

import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorCode;
import com.teamflow.member.dto.InvitationResponse;
import com.teamflow.member.dto.InviteRequest;
import com.teamflow.member.dto.ProjectMemberResponse;
import com.teamflow.user.UserService;
import com.teamflow.user.UserSummary;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
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
    private final SecureRandom random = new SecureRandom();

    public ProjectMemberService(ProjectMemberRepository projectMemberRepository, InvitationRepository invitationRepository,
            UserService userService) {
        this.projectMemberRepository = projectMemberRepository;
        this.invitationRepository = invitationRepository;
        this.userService = userService;
    }

    @Transactional
    public ProjectMember addOwner(Long projectId, Long userId) {
        return projectMemberRepository.save(new ProjectMember(projectId, userId, ProjectRole.OWNER));
    }

    /** @return the requester's membership, once confirmed to be at least {@code minRole}. */
    public ProjectMember requireAtLeast(Long projectId, Long userId, ProjectRole minRole) {
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));
        if (!member.getRole().isAtLeast(minRole)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return member;
    }

    public List<Long> findProjectIdsByUser(Long userId) {
        return projectMemberRepository.findByUserId(userId).stream().map(ProjectMember::getProjectId).toList();
    }

    @Transactional
    public Invitation invite(Long projectId, Long inviterId, InviteRequest request) {
        requireAtLeast(projectId, inviterId, ProjectRole.ADMIN);
        if (request.email() != null) {
            userService.findUserIdByEmail(request.email())
                    .filter(userId -> projectMemberRepository.existsByProjectIdAndUserId(projectId, userId))
                    .ifPresent(userId -> {
                        throw new BusinessException(ErrorCode.ALREADY_MEMBER);
                    });
        }
        ProjectRole role = request.role() != null ? request.role() : ProjectRole.MEMBER;
        Invitation invitation = new Invitation(projectId, request.email(), generateToken(), role, inviterId,
                OffsetDateTime.now().plus(INVITATION_EXPIRY));
        return invitationRepository.save(invitation);
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
        if (projectMemberRepository.existsByProjectIdAndUserId(invitation.getProjectId(), userId)) {
            throw new BusinessException(ErrorCode.ALREADY_MEMBER);
        }
        ProjectMember member = projectMemberRepository.save(new ProjectMember(invitation.getProjectId(), userId, invitation.getRole()));
        invitation.accept();
        return ProjectMemberResponse.from(member, userService.getSummary(userId));
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
        member.changeRole(newRole);
        return ProjectMemberResponse.from(member, userService.getSummary(member.getUserId()));
    }

    @Transactional
    public List<ProjectMemberResponse> transferOwnership(Long projectId, Long requesterId, Long memberId) {
        ProjectMember currentOwner = requireAtLeast(projectId, requesterId, ProjectRole.OWNER);
        ProjectMember newOwner = getMemberInProject(projectId, memberId);
        currentOwner.changeRole(ProjectRole.ADMIN);
        newOwner.changeRole(ProjectRole.OWNER);
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
    }

    @Transactional
    public void leave(Long projectId, Long userId) {
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));
        if (member.getRole() == ProjectRole.OWNER) {
            throw new BusinessException(ErrorCode.OWNER_CANNOT_LEAVE);
        }
        projectMemberRepository.delete(member);
    }

    private ProjectMember getMemberInProject(Long projectId, Long memberId) {
        ProjectMember member = projectMemberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (!member.getProjectId().equals(projectId)) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
        return member;
    }

    private String generateToken() {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
