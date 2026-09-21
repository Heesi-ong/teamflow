package com.teamflow.member.dto;

import com.teamflow.member.ProjectMember;
import com.teamflow.member.ProjectRole;
import com.teamflow.user.UserSummary;
import java.time.OffsetDateTime;

public record ProjectMemberResponse(
        Long id, Long userId, String userName, String userEmail, ProjectRole role, OffsetDateTime joinedAt) {

    public static ProjectMemberResponse from(ProjectMember member, UserSummary user) {
        return new ProjectMemberResponse(member.getId(), member.getUserId(), user.name(), user.email(), member.getRole(), member.getJoinedAt());
    }
}
