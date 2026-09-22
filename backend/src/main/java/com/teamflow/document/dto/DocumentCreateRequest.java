package com.teamflow.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DocumentCreateRequest(@NotBlank @Size(max = 255) String title, String content) {
}
