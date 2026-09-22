package com.teamflow.project.dto;

import com.teamflow.project.ProjectStatus;
import java.time.LocalDate;

// name/description 등은 부분 업데이트라 null 허용(ProjectService에서 공백 문자열만 거른다).
public record ProjectUpdateRequest(String name, String description, ProjectStatus status, LocalDate startDate, LocalDate endDate) {
}
