package com.teamflow.member.dto;

import com.teamflow.member.Invitation;
import com.teamflow.member.ProjectRole;
import java.time.OffsetDateTime;

public record InvitationCreateResponse(Long invitationId, String token, ProjectRole role, OffsetDateTime expiresAt) {

    public static InvitationCreateResponse from(Invitation invitation) {
        return new InvitationCreateResponse(invitation.getId(), invitation.getToken(), invitation.getRole(), invitation.getExpiresAt());
    }
}
