package com.teamflow.member.dto;

import com.teamflow.member.ProjectRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record InviteRequest(@Email @Size(max = 255) String email, ProjectRole role) {
}
