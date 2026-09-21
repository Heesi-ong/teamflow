package com.teamflow.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Full catalog of API error codes, per 18-error-handling-policy.md and
 * every `Error:` line in 08-api-specification.md. New codes are added
 * here first so the two documents and the code never drift apart again.
 */
public enum ErrorCode {

    // Common
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

    // Auth
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    INVALID_PASSWORD_FORMAT(HttpStatus.BAD_REQUEST, "비밀번호 형식이 올바르지 않습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Refresh Token이 유효하지 않습니다."),

    // Project
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "프로젝트를 찾을 수 없습니다."),

    // Member / Invitation
    ALREADY_MEMBER(HttpStatus.CONFLICT, "이미 프로젝트에 참여 중입니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "팀원을 찾을 수 없습니다."),
    OWNER_CANNOT_LEAVE(HttpStatus.CONFLICT, "OWNER는 소유권을 위임한 뒤 탈퇴할 수 있습니다."),
    INVITATION_NOT_FOUND(HttpStatus.NOT_FOUND, "초대를 찾을 수 없습니다."),
    INVITATION_EXPIRED(HttpStatus.GONE, "만료되었거나 더 이상 유효하지 않은 초대입니다."),

    // Task
    TASK_NOT_FOUND(HttpStatus.NOT_FOUND, "Task를 찾을 수 없습니다."),
    INVALID_TASK_STATUS(HttpStatus.BAD_REQUEST, "Task 상태 값이 올바르지 않습니다."),
    TASK_VERSION_CONFLICT(HttpStatus.CONFLICT, "다른 사용자가 먼저 이 Task를 수정했습니다. 다시 조회한 뒤 시도하세요."),

    // File
    INVALID_FILE(HttpStatus.BAD_REQUEST, "허용되지 않는 파일 형식이거나 크기 제한을 초과했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
