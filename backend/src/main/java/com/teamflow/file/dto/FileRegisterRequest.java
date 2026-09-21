package com.teamflow.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FileRegisterRequest(
        @NotBlank String s3Key, @NotBlank String fileName, @NotNull @Positive Long fileSize, @NotBlank String contentType,
        Long taskId) {
}
