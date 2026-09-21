package com.teamflow.common.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/** 08-api-specification.md 공통 규칙: 목록 조회 응답은 `{ content, page, size, totalElements }`. */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements) {

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }
}
