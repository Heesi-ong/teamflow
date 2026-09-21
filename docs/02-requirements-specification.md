# 02. Requirements Specification

Requirement ID 규칙: `FR-{DOMAIN}-{번호}` (Functional), `NFR-{CATEGORY}-{번호}` (Non-Functional).

## 1. Functional Requirements

### FR-AUTH (인증)

| ID | 내용 | 상태 |
|---|---|---|
| FR-AUTH-001 | 사용자는 이메일/비밀번호로 회원가입할 수 있다 | Core |
| FR-AUTH-002 | 사용자는 이메일/비밀번호로 로그인할 수 있다 | Core |
| FR-AUTH-003 | 로그인 성공 시 Access Token, Refresh Token을 발급한다 | Core |
| FR-AUTH-004 | Access Token 만료 시 Refresh Token으로 재발급할 수 있다 | Core |
| FR-AUTH-005 | 사용자는 로그아웃할 수 있으며, 로그아웃 시 Refresh Token은 무효화된다 | Core |
| FR-AUTH-006 | 비밀번호는 해시(BCrypt)로 저장한다 | Core |
| FR-AUTH-007 | 사용자는 프로필 이미지를 업로드할 수 있다 (S3 Presigned URL) | Optional |

### FR-PROJECT (프로젝트)

| ID | 내용 | 상태 |
|---|---|---|
| FR-PROJECT-001 | 사용자는 프로젝트를 생성할 수 있다 (생성자는 OWNER가 된다) | Core |
| FR-PROJECT-002 | OWNER/ADMIN은 프로젝트 정보를 수정할 수 있다 | Core |
| FR-PROJECT-003 | OWNER는 프로젝트를 삭제할 수 있다 (Soft Delete, `deleted_at` 설정) | Core |
| FR-PROJECT-004 | 프로젝트 멤버는 참여 중인 프로젝트 목록을 조회할 수 있다 | Core |
| FR-PROJECT-005 | 프로젝트는 상태(PLANNING/IN_PROGRESS/ON_HOLD/COMPLETED/ARCHIVED)를 가진다 | Core |

### FR-MEMBER (팀원)

| ID | 내용 | 상태 |
|---|---|---|
| FR-MEMBER-001 | OWNER/ADMIN은 이메일 또는 초대 링크로 팀원을 초대할 수 있다 | Core |
| FR-MEMBER-002 | 초대받은 사용자는 초대를 수락하여 프로젝트에 참가할 수 있다 | Core |
| FR-MEMBER-003 | OWNER/ADMIN은 팀원을 제거할 수 있다 | Core |
| FR-MEMBER-004 | OWNER는 팀원의 Role을 변경할 수 있다 | Core |
| FR-MEMBER-005 | 팀원은 프로젝트에서 스스로 탈퇴할 수 있다 (OWNER는 탈퇴 불가, 위임 후 가능) | Core |
| FR-MEMBER-006 | OWNER는 프로젝트 소유권을 다른 팀원에게 위임할 수 있다 (위임 대상은 OWNER로, 기존 OWNER는 ADMIN으로 전환) | Core |
| FR-MEMBER-007 | OWNER/ADMIN은 대기 중인 초대 목록을 조회하고 취소할 수 있다 | Core |

### FR-TASK (Task)

| ID | 내용 | 상태 |
|---|---|---|
| FR-TASK-001 | MEMBER 이상은 Task를 생성할 수 있다 | Core |
| FR-TASK-002 | Task 작성자, 담당자, ADMIN 이상은 Task를 수정할 수 있다 | Core |
| FR-TASK-003 | Task 작성자, ADMIN 이상은 Task를 삭제할 수 있다 | Core |
| FR-TASK-004 | Task는 상태(TODO/IN_PROGRESS/REVIEW/DONE)를 가진다 | Core |
| FR-TASK-005 | Task는 우선순위(LOW/MEDIUM/HIGH/URGENT)를 가진다 | Core |
| FR-TASK-006 | Task에는 담당자를 지정할 수 있다 | Core |
| FR-TASK-007 | Task는 Checklist 항목을 가질 수 있다 | Core |
| FR-TASK-008 | Task는 Kanban Board에서 Drag & Drop으로 상태 변경이 가능하다 | Core |
| FR-TASK-009 | Task에 파일을 첨부할 수 있다 | Core |
| FR-TASK-010 | 두 사용자가 동일 Task를 동시에 수정하면 낙관적 락(`version`)으로 충돌을 감지해 나중 요청을 거부한다 | Core |

### FR-COMMENT (댓글)

| ID | 내용 | 상태 |
|---|---|---|
| FR-COMMENT-001 | 팀원은 Task에 댓글을 작성할 수 있다 | Core |
| FR-COMMENT-002 | 댓글에서 `@username`으로 다른 팀원을 Mention할 수 있다 | Core |
| FR-COMMENT-003 | Mention 발생 시 대상자에게 Notification이 생성된다 | Core |

### FR-NOTIFICATION (알림)

| ID | 내용 | 상태 |
|---|---|---|
| FR-NOTIFICATION-001 | Task 배정/상태변경/Mention/초대/댓글/마감임박 발생 시 Notification을 생성한다 | Core |
| FR-NOTIFICATION-002 | Notification은 SSE로 실시간 전달된다 | Core |
| FR-NOTIFICATION-003 | 사용자는 Notification을 읽음 처리할 수 있다 | Core |

### FR-CHAT (채팅)

| ID | 내용 | 상태 |
|---|---|---|
| FR-CHAT-001 | 프로젝트 팀원은 프로젝트 채팅방에서 메시지를 주고받을 수 있다 | Core |
| FR-CHAT-002 | 채팅은 WebSocket을 통해 실시간으로 전달된다 | Core |
| FR-CHAT-003 | 채팅 이력은 조회할 수 있다 | Core |

### FR-DOCUMENT (문서)

| ID | 내용 | 상태 |
|---|---|---|
| FR-DOCUMENT-001 | 팀원은 프로젝트 문서를 작성/수정할 수 있다 | Core |
| FR-DOCUMENT-002 | 문서에는 첨부파일을 추가할 수 있다 | Optional |

### FR-FILE (파일)

| ID | 내용 | 상태 |
|---|---|---|
| FR-FILE-001 | 팀원은 Presigned URL을 통해 S3에 직접 파일을 업로드할 수 있다 | Core |
| FR-FILE-002 | 업로드 완료 후 파일 메타데이터를 백엔드에 등록한다 | Core |

### FR-ACTIVITY (활동 기록)

| ID | 내용 | 상태 |
|---|---|---|
| FR-ACTIVITY-001 | Task 생성/상태변경, 멤버 변경 등 주요 이벤트는 ActivityLog에 자동 기록된다 | Core |

### FR-DASHBOARD (대시보드)

| ID | 내용 | 상태 |
|---|---|---|
| FR-DASHBOARD-001 | 프로젝트별 Task 통계(전체/완료/진행중/미진행), 진행률, 마감임박 Task, 팀원 수를 제공한다 | Core |

### FR-SEARCH (검색)

| ID | 내용 | 상태 |
|---|---|---|
| FR-SEARCH-001 | Task/Document/Comment를 키워드로 검색할 수 있다 (PostgreSQL 기반) | Core |
| FR-SEARCH-002 | Elasticsearch 기반 검색으로 확장할 수 있다 | Optional |

## 2. Non-Functional Requirements

| ID | 내용 | 상태 |
|---|---|---|
| NFR-PERFORMANCE-001 | 주요 API 응답 시간은 P95 기준 300ms 이내를 목표로 한다 | Core |
| NFR-PERFORMANCE-002 | Dashboard/Project 조회는 Redis 캐시를 통해 DB 부하를 감소시킨다 | Core |
| NFR-SECURITY-001 | 모든 API 통신은 HTTPS를 사용한다 | Core |
| NFR-SECURITY-002 | 비밀번호는 BCrypt로 해시하여 저장한다 | Core |
| NFR-SECURITY-003 | JWT 기반 인증과 RBAC 기반 인가를 모든 API에 적용한다 | Core |
| NFR-SECURITY-004 | Rate Limiting을 통해 API 남용을 방지한다 | Optional |
| NFR-AVAILABILITY-001 | Health Check를 통해 배포 시 무중단 목표를 지향한다 | Optional |
| NFR-SCALABILITY-001 | Modular Monolith 구조로 향후 MSA 분리가 가능하도록 설계한다 | Planned |
| NFR-OBSERVABILITY-001 | Prometheus/Grafana로 API 지표와 시스템 리소스를 모니터링한다 | Core |
| NFR-TESTABILITY-001 | Service/Controller/E2E Test를 통해 핵심 시나리오를 자동 검증한다 | Core |
| NFR-MAINTAINABILITY-001 | Domain 별로 Package를 분리한 Modular Monolith 구조를 유지한다 | Core |

## 3. Role별 Requirement

| Role | 주요 Requirement |
|---|---|
| OWNER | FR-PROJECT-002/003, FR-MEMBER-001~004 |
| ADMIN | FR-MEMBER-001~003, FR-TASK-002/003, FR-DOCUMENT-001 |
| MEMBER | FR-TASK-001~009, FR-COMMENT-001~003, FR-FILE-001, FR-DOCUMENT-001 |
| GUEST | 조회 중심 (FR-TASK 조회, FR-COMMENT-001 제한적 허용) |

Role별 상세 권한 매트릭스는 [09-authentication-authorization.md](./09-authentication-authorization.md)를 참고한다.

## 4. 시스템 제약사항

- 초기 배포 환경은 단일 AWS EC2 인스턴스(또는 동급 Cloud Server) 기준으로 설계한다.
- Backend는 Modular Monolith 단일 애플리케이션으로 운영한다 (MSA 미도입, [04-system-architecture.md](./04-system-architecture.md) 참고).
- 파일은 Local Storage가 아닌 AWS S3에만 저장한다.
- 검색은 초기 PostgreSQL 기반으로 구현하며 Elasticsearch는 확장 옵션으로 둔다.
