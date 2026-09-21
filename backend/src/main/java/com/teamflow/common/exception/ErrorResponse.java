package com.teamflow.common.exception;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Common error response shape, per 18-error-handling-policy.md §1.
 */
public record ErrorResponse(
        String code,
        String message,
        OffsetDateTime timestamp,
        List<FieldError> errors
) {

    public record FieldError(String field, String reason) {
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), OffsetDateTime.now(), null);
    }

    public static ErrorResponse of(ErrorCode errorCode, List<FieldError> errors) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), OffsetDateTime.now(), errors);
    }
}
