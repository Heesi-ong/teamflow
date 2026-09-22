package com.teamflow.common.exception;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

/**
 * Single place that turns exceptions into the common error response format
 * (18-error-handling-policy.md). Internal details (stack traces, SQL) are
 * never included in the response body — see 17-security-design.md §11.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        ErrorCode errorCode = ErrorCode.TASK_VERSION_CONFLICT;
        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return ResponseEntity.status(ErrorCode.INVALID_REQUEST.getStatus())
                .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, fieldErrors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        // Malformed JSON or an invalid enum literal (e.g. status: "BOGUS") lands here before
        // it ever reaches @Valid — without this it fell through to the generic 500 below.
        return ResponseEntity.status(ErrorCode.INVALID_REQUEST.getStatus()).body(ErrorResponse.of(ErrorCode.INVALID_REQUEST));
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleDisconnectedAsyncClient(AsyncRequestNotUsableException ex) {
        // SSE 클라이언트가 탭을 닫은 뒤 컨테이너가 연결 종료를 알릴 때 발생하는 정상적인 연결 수명
        // 이벤트다. 이미 text/event-stream 응답이 커밋됐으므로 JSON 오류 본문을 쓰려고 하면 오히려
        // HttpMessageNotWritableException과 대량의 스택 트레이스가 남는다.
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        // Framework exceptions (404 on an unmapped route, 405 on a wrong
        // method, etc.) implement this interface and already carry the
        // right status — pass it through instead of flattening to 500.
        if (ex instanceof org.springframework.web.ErrorResponse errorResponse) {
            return ResponseEntity.status(errorResponse.getStatusCode())
                    .body(new ErrorResponse(errorResponse.getStatusCode().toString(),
                            errorResponse.getBody().getDetail(), OffsetDateTime.now(), null));
        }
        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
