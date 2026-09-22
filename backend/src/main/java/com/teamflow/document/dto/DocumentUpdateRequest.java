package com.teamflow.document.dto;

import jakarta.validation.constraints.Size;

// title/content는 부분 업데이트라 null 허용(DocumentService에서 공백 문자열만 거른다).
public record DocumentUpdateRequest(@Size(max = 255) String title, String content) {
}
