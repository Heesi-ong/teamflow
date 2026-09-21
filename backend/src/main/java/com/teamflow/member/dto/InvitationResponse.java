package com.teamflow.member.dto;

import com.teamflow.member.Invitation;
import com.teamflow.member.InvitationStatus;
import com.teamflow.member.ProjectRole;
import java.time.OffsetDateTime;

/** Token is intentionally omitted — 08-api-specification.md §3: "token은 노출하지 않음". */
public record InvitationResponse(
        Long id, String email, ProjectRole role, InvitationStatus status, OffsetDateTime expiresAt, OffsetDateTime createdAt) {

    public static InvitationResponse from(Invitation invitation) {
        return new InvitationResponse(invitation.getId(), invitation.getEmail(), invitation.getRole(),
                invitation.getStatus(), invitation.getExpiresAt(), invitation.getCreatedAt());
    }
}
