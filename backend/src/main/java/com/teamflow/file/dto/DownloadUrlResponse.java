package com.teamflow.file.dto;

public record DownloadUrlResponse(String presignedUrl, long expiresIn) {
}
