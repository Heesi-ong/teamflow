package com.teamflow.project.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record ProjectCreateRequest(@NotBlank String name, String description, LocalDate startDate, LocalDate endDate) {
}
