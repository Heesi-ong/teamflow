package com.teamflow.member;

import com.teamflow.auth.UserPrincipal;
import com.teamflow.member.dto.ChangeRoleRequest;
import com.teamflow.member.dto.InvitationCreateResponse;
import com.teamflow.member.dto.InvitationResponse;
import com.teamflow.member.dto.InviteRequest;
import com.teamflow.member.dto.ProjectMemberResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 08-api-specification.md §3 Member. */
@RestController
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    public ProjectMemberController(ProjectMemberService projectMemberService) {
        this.projectMemberService = projectMemberService;
    }

    @PostMapping("/api/projects/{projectId}/invitations")
    public ResponseEntity<InvitationCreateResponse> invite(
            @PathVariable Long projectId, @AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody InviteRequest request) {
        Invitation invitation = projectMemberService.invite(projectId, principal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(InvitationCreateResponse.from(invitation));
    }

    @GetMapping("/api/projects/{projectId}/invitations")
    public List<InvitationResponse> listInvitations(
            @PathVariable Long projectId, @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) InvitationStatus status) {
        return projectMemberService.listInvitations(projectId, principal.userId(), status);
    }

    @DeleteMapping("/api/projects/{projectId}/invitations/{invitationId}")
    public ResponseEntity<Void> revokeInvitation(
            @PathVariable Long projectId, @PathVariable Long invitationId, @AuthenticationPrincipal UserPrincipal principal) {
        projectMemberService.revokeInvitation(projectId, principal.userId(), invitationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/projects/{projectId}/members")
    public List<ProjectMemberResponse> listMembers(@PathVariable Long projectId, @AuthenticationPrincipal UserPrincipal principal) {
        return projectMemberService.listMembers(projectId, principal.userId());
    }

    @PatchMapping("/api/projects/{projectId}/members/{memberId}/role")
    public ProjectMemberResponse changeRole(
            @PathVariable Long projectId, @PathVariable Long memberId,
            @AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody ChangeRoleRequest request) {
        return projectMemberService.changeRole(projectId, principal.userId(), memberId, request.role());
    }

    @DeleteMapping("/api/projects/{projectId}/members/me")
    public ResponseEntity<Void> leave(@PathVariable Long projectId, @AuthenticationPrincipal UserPrincipal principal) {
        projectMemberService.leave(projectId, principal.userId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/projects/{projectId}/members/{memberId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long projectId, @PathVariable Long memberId, @AuthenticationPrincipal UserPrincipal principal) {
        projectMemberService.removeMember(projectId, principal.userId(), memberId);
        return ResponseEntity.noContent().build();
    }
}
