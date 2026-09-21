package com.teamflow.project.dto;

import com.teamflow.project.Project;
import com.teamflow.project.ProjectStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ProjectResponse(
        Long id, String name, String description, ProjectStatus status,
        LocalDate startDate, LocalDate endDate, Long ownerId, OffsetDateTime createdAt, OffsetDateTime updatedAt) {

    public static ProjectResponse from(Project project) {
        return new ProjectResponse(project.getId(), project.getName(), project.getDescription(), project.getStatus(),
                project.getStartDate(), project.getEndDate(), project.getOwnerId(), project.getCreatedAt(), project.getUpdatedAt());
    }
}
