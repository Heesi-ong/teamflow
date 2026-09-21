# 19. Logging & Audit Policy

## 1. Application Log vs Audit Log

| 구분 | 목적 | 저장 위치 | 보관 기간 |
|---|---|---|---|
| Application Log | 장애 진단, 디버깅, 요청 추적 | 파일/stdout (Docker logging driver) | 단기(예: 14일 롤링) |
| Audit Log (ActivityLog) | 누가 무엇을 언제 변경했는지 증빙 | PostgreSQL (`activity_logs` 테이블) | 영구 보관 |

Application Log는 운영/디버깅용 휘발성 로그이고, Audit Log는 [07-database-design.md](./07-database-design.md)의 `ActivityLog` 엔티티로 영구 보관되는 별개의 개념이다.

## 2. Application Log Level

| Level | 사용 기준 |
|---|---|
| ERROR | 처리 불가능한 예외, 500 응답으로 이어지는 오류 |
| WARN | 비정상이지만 처리는 가능한 상황 (예: 만료된 토큰으로 재요청, 재시도 성공) |
| INFO | 주요 비즈니스 이벤트 (요청 시작/종료, 배포/기동 정보) |
| DEBUG | 개발 중 상세 흐름 추적 (운영 환경 기본 비활성화) |

- 모든 요청에 Request ID(UUID)를 부여하고(`MDC`), 로그 라인마다 포함시켜 하나의 요청 흐름을 추적 가능하게 한다.
- 포맷: `[{timestamp}] [{level}] [{requestId}] {logger} - {message}`

## 3. Audit Log 대상 (ActivityLog)

다음 이벤트는 반드시 `activity_logs`에 기록한다.

| 이벤트 | action_type |
|---|---|
| Task 생성 | `TASK_CREATED` |
| Task 상태 변경 | `TASK_STATUS_CHANGED` |
| Task 담당자 변경 | `TASK_ASSIGNEE_CHANGED` |
| Task 삭제 | `TASK_DELETED` |
| 팀원 초대/참가 | `MEMBER_INVITED` / `MEMBER_JOINED` |
| 팀원 Role 변경 | `MEMBER_ROLE_CHANGED` |
| 팀원 제거/탈퇴 | `MEMBER_REMOVED` / `MEMBER_LEFT` |
| 프로젝트 정보/상태 변경 | `PROJECT_UPDATED` |
| 문서 생성/수정/삭제 | `DOCUMENT_CREATED` / `DOCUMENT_UPDATED` / `DOCUMENT_DELETED` |

메시지 예시:

```
사용자가 Task를 생성했습니다.
사용자가 Task 상태를 IN_PROGRESS → DONE으로 변경했습니다.
ADMIN이 새로운 Member를 초대했습니다.
```

## 4. Sensitive Data 처리

다음 데이터는 Application Log와 Audit Log 어디에도 원문으로 기록하지 않는다.

- 비밀번호 (평문/해시 모두 금지)
- Access Token / Refresh Token 원문
- 이메일 전체(로그 상 필요 시 마스킹, 예: `us***@example.com`)
- 카드/결제 정보 (TeamFlow 범위에는 해당 없음, 향후 결제 기능 도입 시 원칙 적용)

요청/응답 로깅 시 `password`, `token`, `Authorization` 필드는 로깅 필터에서 자동 마스킹 처리한다.

## 5. 로그 수집/보관 (운영 환경)

- 컨테이너 표준출력(stdout)으로 로그를 출력하고 Docker의 `json-file` 드라이버(rotation 설정 포함)로 관리한다.
- ELK/Loki 등 중앙 로그 수집 시스템 연동은 트래픽 증가 시 도입한다 — 상태: Optional.
