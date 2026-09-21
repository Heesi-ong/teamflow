package com.teamflow.project.dto;

import com.teamflow.project.ProjectStatus;
import java.time.LocalDate;

public record ProjectUpdateRequest(String name, String description, ProjectStatus status, LocalDate startDate, LocalDate endDate) {
}
