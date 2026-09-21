package com.teamflow.member;

import com.teamflow.auth.UserPrincipal;
import com.teamflow.member.dto.ProjectMemberResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** 08-api-specification.md §3 — top-level, not scoped under a projectId (the token identifies the project). */
@RestController
public class InvitationController {

    private final ProjectMemberService projectMemberService;

    public InvitationController(ProjectMemberService projectMemberService) {
        this.projectMemberService = projectMemberService;
    }

    @PostMapping("/api/invitations/{token}/accept")
    public ProjectMemberResponse accept(@PathVariable String token, @AuthenticationPrincipal UserPrincipal principal) {
        return projectMemberService.acceptInvitation(token, principal.userId());
    }
}
