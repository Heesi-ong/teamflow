package com.teamflow.file.dto;

public record PresignedUploadResponse(String presignedUrl, String s3Key, long expiresIn) {
}
