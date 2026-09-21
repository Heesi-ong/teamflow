package com.teamflow.member.dto;

import com.teamflow.member.ProjectRole;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(@NotNull ProjectRole role) {
}
