package com.teamflow.document.dto;

// title/content는 부분 업데이트라 null 허용(DocumentService에서 공백 문자열만 거른다).
public record DocumentUpdateRequest(String title, String content) {
}
