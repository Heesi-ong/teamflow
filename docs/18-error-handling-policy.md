# 18. Error Handling Policy

## 1. 공통 Error Response 형식

모든 에러 응답은 다음 형식을 따른다. `@RestControllerAdvice` 하나에서 `BusinessException`(및 하위 타입)과 `MethodArgumentNotValidException` 등 공통 예외를 일괄 처리한다.

```json
{
  "code": "PROJECT_NOT_FOUND",
  "message": "프로젝트를 찾을 수 없습니다.",
  "timestamp": "2026-09-21T10:00:00Z"
}
```

Validation 오류처럼 필드 단위 상세가 필요한 경우 `errors` 배열을 추가한다.

```json
{
  "code": "INVALID_REQUEST",
  "message": "요청 값이 올바르지 않습니다.",
  "timestamp": "2026-09-21T10:00:00Z",
  "errors": [
    { "field": "email", "reason": "이메일 형식이 아닙니다." }
  ]
}
```

## 2. Error Code Naming Convention

- 형식: `대문자 SNAKE_CASE`, `{리소스}_{문제}` 순서
- 예: `TASK_NOT_FOUND`, `PROJECT_NOT_FOUND`, `EMAIL_ALREADY_EXISTS`, `INVALID_TASK_STATUS`, `FORBIDDEN`, `UNAUTHORIZED`
- 리소스를 특정할 수 없는 공통 오류는 `INVALID_REQUEST`, `INTERNAL_SERVER_ERROR`를 사용한다.

## 3. HTTP Status 매핑 원칙

| 상황 | HTTP Status | 예시 Code |
|---|---|---|
| 인증 실패/토큰 없음 | 401 | `UNAUTHORIZED`, `INVALID_CREDENTIALS` |
| 권한 없음 | 403 | `FORBIDDEN` |
| 리소스 없음 | 404 | `TASK_NOT_FOUND`, `PROJECT_NOT_FOUND` |
| 입력값 오류 | 400 | `INVALID_REQUEST`, `INVALID_TASK_STATUS` |
| 리소스 충돌/중복 | 409 | `EMAIL_ALREADY_EXISTS`, `ALREADY_MEMBER` |
| 만료된 리소스 | 410 | `INVITATION_EXPIRED` |
| 서버 오류 | 500 | `INTERNAL_SERVER_ERROR` |

## 4. Custom Exception 설계

```java
public abstract class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
    protected BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}

public enum ErrorCode {
    TASK_NOT_FOUND(HttpStatus.NOT_FOUND, "Task를 찾을 수 없습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다.");
    // ...
}
```

- Module별로 별도 Exception 클래스를 늘리지 않고, `ErrorCode` Enum 하나에 코드/상태/메시지를 등록하고 `BusinessException(errorCode)`로 던지는 방식을 기본으로 한다.
- Module 고유의 흐름 제어가 필요한 경우에만 별도 Exception 클래스를 추가한다 (예: 재시도가 필요한 경우).

## 5. 서버 내부 오류 노출 금지

- 500 오류 발생 시 클라이언트에는 `INTERNAL_SERVER_ERROR`와 고정 메시지만 반환하고, 스택 트레이스/쿼리 원문/내부 클래스명은 노출하지 않는다.
- 상세 원인은 서버 로그에만 기록한다 ([19-logging-audit-policy.md](./19-logging-audit-policy.md) 참고).

## 6. Frontend 처리 원칙

- axios interceptor에서 공통 Error Response를 파싱하여 `code` 기준으로 분기 처리한다 (예: `UNAUTHORIZED` → 로그인 페이지 이동, `FORBIDDEN` → 접근 불가 안내).
- `message` 필드는 사용자에게 바로 노출 가능한 한국어 메시지로 설계하므로 Frontend는 별도 메시지 매핑 없이 그대로 표시할 수 있다.
