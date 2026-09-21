package com.teamflow.common.exception;

/**
 * Base exception for all expected business-rule failures. Services throw
 * `new BusinessException(ErrorCode.X)` and GlobalExceptionHandler turns it
 * into the common error response — see 18-error-handling-policy.md.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
