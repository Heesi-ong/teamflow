package com.teamflow.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record FileRegisterRequest(
        @NotBlank @Size(max = 500) String s3Key, @NotBlank @Size(max = 255) String fileName,
        @NotNull @Positive Long fileSize, @NotBlank @Size(max = 100) String contentType,
        Long taskId) {
}
