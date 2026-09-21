package com.teamflow.project.dto;

import com.teamflow.project.Project;
import com.teamflow.project.ProjectStatus;
import java.time.LocalDate;

public record ProjectSummaryResponse(Long id, String name, String description, ProjectStatus status, LocalDate startDate, LocalDate endDate) {

    public static ProjectSummaryResponse from(Project project) {
        return new ProjectSummaryResponse(project.getId(), project.getName(), project.getDescription(),
                project.getStatus(), project.getStartDate(), project.getEndDate());
    }
}
