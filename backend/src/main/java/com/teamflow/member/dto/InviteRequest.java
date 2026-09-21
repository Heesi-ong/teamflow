package com.teamflow.member.dto;

import com.teamflow.member.ProjectRole;
import jakarta.validation.constraints.Email;

public record InviteRequest(@Email String email, ProjectRole role) {
}
