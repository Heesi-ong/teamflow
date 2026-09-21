# 03. Functional Specification

각 기능은 `기능명 / 목적 / 사용자 / 사전 조건 / 입력값 / 처리 과정 / 출력값 / 예외 상황 / 관련 API / 관련 Entity` 형식으로 기술한다. Requirement ID는 [02-requirements-specification.md](./02-requirements-specification.md) 참고.

---

## 3.1 회원가입 (FR-AUTH-001)

- **목적**: 신규 사용자가 서비스를 이용할 수 있도록 계정을 생성한다.
- **사용자**: 비로그인 사용자
- **사전 조건**: 없음
- **입력값**: email, password, name
- **처리 과정**:
  1. email 중복 여부 확인
  2. password 형식 검증 (8자 이상, 영문+숫자 포함)
  3. password를 BCrypt로 해시하여 User 저장
- **출력값**: 생성된 User 요약 정보 (id, email, name)
- **예외 상황**: email 중복(`EMAIL_ALREADY_EXISTS`), 비밀번호 형식 오류(`INVALID_PASSWORD_FORMAT`)
- **관련 API**: `POST /api/auth/signup`
- **관련 Entity**: User

---

## 3.2 로그인 / 토큰 발급 (FR-AUTH-002, FR-AUTH-003)

- **목적**: 사용자를 인증하고 API 호출에 필요한 JWT를 발급한다.
- **사용자**: 비로그인 사용자
- **사전 조건**: 회원가입 완료
- **입력값**: email, password
- **처리 과정**:
  1. email로 User 조회
  2. password 일치 여부 검증 (BCrypt)
  3. Access Token(단기), Refresh Token(장기) 발급
  4. Refresh Token을 Redis에 저장 (TTL 적용)
- **출력값**: accessToken, refreshToken, 만료 시각
- **예외 상황**: 인증 실패(`INVALID_CREDENTIALS`)
- **관련 API**: `POST /api/auth/login`
- **관련 Entity**: User

---

## 3.3 Token 재발급 (FR-AUTH-004)

- **목적**: Access Token 만료 시 재로그인 없이 세션을 유지한다.
- **사용자**: 로그인 사용자 (Refresh Token 보유)
- **사전 조건**: 유효한 Refresh Token 존재
- **입력값**: refreshToken
- **처리 과정**:
  1. Redis에서 Refresh Token 유효성 확인
  2. 신규 Access Token 발급 (Refresh Token Rotation 적용 시 신규 Refresh Token도 발급)
- **출력값**: 신규 accessToken (및 신규 refreshToken)
- **예외 상황**: Refresh Token 만료/위조(`INVALID_REFRESH_TOKEN`)
- **관련 API**: `POST /api/auth/refresh`
- **관련 Entity**: User (Redis: refresh token 저장)

---

## 3.4 로그아웃 (FR-AUTH-005)

- **목적**: 세션을 종료하고 Refresh Token을 무효화한다.
- **사용자**: 로그인 사용자
- **사전 조건**: 로그인 상태
- **입력값**: Access Token (Header)
- **처리 과정**: Redis에서 해당 사용자의 Refresh Token 삭제
- **출력값**: 204 No Content
- **예외 상황**: 미인증 요청(`UNAUTHORIZED`)
- **관련 API**: `POST /api/auth/logout`
- **관련 Entity**: User (Redis)

---

## 3.5 프로젝트 생성 (FR-PROJECT-001)

- **목적**: 새로운 협업 프로젝트를 생성한다.
- **사용자**: 로그인 사용자
- **사전 조건**: 없음
- **입력값**: name, description, startDate, endDate
- **처리 과정**:
  1. Project 저장
  2. 생성자를 ProjectMember(Role=OWNER)로 등록
  3. ActivityLog 기록
- **출력값**: 생성된 Project 정보
- **예외 상황**: 필수값 누락(`INVALID_REQUEST`)
- **관련 API**: `POST /api/projects`
- **관련 Entity**: Project, ProjectMember, ActivityLog

---

## 3.6 팀원 초대 (FR-MEMBER-001, FR-MEMBER-002, FR-MEMBER-007)

- **목적**: 프로젝트에 새로운 팀원을 참여시킨다.
- **사용자**: OWNER, ADMIN
- **사전 조건**: 프로젝트 존재, 요청자가 OWNER/ADMIN
- **입력값**: 초대 대상 email(선택) 또는 초대 링크 요청, 부여할 role(기본값 MEMBER)
- **처리 과정**:
  1. 권한 확인 (OWNER/ADMIN)
  2. Invitation 저장(status=PENDING, token 발급, 만료 시각 설정) 및 email 발송 또는 링크 반환
  3. 대상자가 초대 수락 시 Invitation을 조회해 유효성(PENDING·미만료) 확인 후 ProjectMember(Invitation.role) 생성, Invitation.status=ACCEPTED로 전환
  4. Notification 생성 (초대), ActivityLog 기록
  5. OWNER/ADMIN은 대기 중인 초대 목록을 조회하고, 필요 시 취소(status=REVOKED)할 수 있다
- **출력값**: 초대 결과(초대 목록 포함) / 참가 완료된 ProjectMember 정보
- **예외 상황**: 권한 없음(`FORBIDDEN`), 이미 참여 중(`ALREADY_MEMBER`), 만료·취소·이미 사용된 초대(`INVITATION_EXPIRED`), 존재하지 않는 초대(`INVITATION_NOT_FOUND`)
- **관련 API**: `POST /api/projects/{projectId}/invitations`, `GET /api/projects/{projectId}/invitations`, `DELETE /api/projects/{projectId}/invitations/{invitationId}`, `POST /api/invitations/{token}/accept`
- **관련 Entity**: Invitation, ProjectMember, Notification, ActivityLog

---

## 3.7 Task 생성 (FR-TASK-001)

- **목적**: 프로젝트 내 업무 단위를 생성한다.
- **사용자**: MEMBER 이상
- **사전 조건**: 요청자가 ProjectMember, Role이 MEMBER 이상
- **입력값**: title, description, assigneeId(optional), priority, dueDate
- **처리 과정**:
  1. ProjectMember 및 Role 검증
  2. Task 저장 (기본 상태 TODO)
  3. 담당자 지정 시 TaskAssignee 생성
  4. ActivityLog 기록
  5. 담당자에게 Notification 생성 및 SSE 전송
- **출력값**: 생성된 Task 정보
- **예외 상황**: 권한 없음(`FORBIDDEN`), 존재하지 않는 담당자(`MEMBER_NOT_FOUND`)
- **관련 API**: `POST /api/projects/{projectId}/tasks`
- **관련 Entity**: Task, TaskAssignee, ActivityLog, Notification

전체 흐름은 [08. Task 생성 Flow](./04-system-architecture.md#task-생성-flow) 참고.

---

## 3.8 Task 상태 변경 (FR-TASK-004, FR-TASK-008, FR-TASK-010)

- **목적**: Kanban Board에서 Task 진행 상태를 변경한다.
- **사용자**: 작성자, 담당자, ADMIN 이상
- **사전 조건**: Task 존재
- **입력값**: status (TODO/IN_PROGRESS/REVIEW/DONE), version (클라이언트가 마지막으로 조회한 버전)
- **처리 과정**:
  1. 권한 검증 (작성자/담당자/ADMIN 이상)
  2. 요청 `version`과 현재 Task.version 비교(낙관적 락) — 불일치 시 4로 진행하지 않고 충돌 처리
  3. 상태 변경 및 저장(저장 시 `version` 자동 증가)
  4. ActivityLog 기록 (변경 전/후 상태 포함)
  5. 담당자 외 관련자에게 Notification 생성
- **출력값**: 변경된 Task 정보(갱신된 version 포함)
- **예외 상황**: 권한 없음(`FORBIDDEN`), 잘못된 상태값(`INVALID_TASK_STATUS`), 동시 수정 충돌(`TASK_VERSION_CONFLICT`) — Frontend는 충돌 시 Task를 재조회한 뒤 재시도를 안내한다
- **관련 API**: `PATCH /api/projects/{projectId}/tasks/{taskId}/status`
- **관련 Entity**: Task, ActivityLog, Notification

---

## 3.9 Task 담당자 지정 (FR-TASK-006)

- **목적**: Task를 수행할 팀원을 지정한다.
- **사용자**: 작성자, ADMIN 이상
- **사전 조건**: 대상자가 해당 프로젝트의 ProjectMember
- **입력값**: assigneeId
- **처리 과정**: TaskAssignee 생성/교체, ActivityLog 기록, Notification 생성
- **출력값**: 변경된 담당자 정보
- **예외 상황**: 비멤버 지정 시도(`MEMBER_NOT_FOUND`)
- **관련 API**: `PATCH /api/projects/{projectId}/tasks/{taskId}/assignee`
- **관련 Entity**: TaskAssignee, Notification

---

## 3.10 Checklist 관리 (FR-TASK-007)

- **목적**: Task 내부 세부 작업 항목을 관리한다.
- **사용자**: 작성자, 담당자, MEMBER 이상
- **사전 조건**: Task 존재
- **입력값**: content, isDone
- **처리 과정**: TaskChecklist 생성/수정/삭제, 완료율 재계산
- **출력값**: Checklist 목록 및 완료율
- **예외 상황**: 권한 없음(`FORBIDDEN`)
- **관련 API**: `POST /api/tasks/{taskId}/checklists`, `PATCH /api/tasks/{taskId}/checklists/{checklistId}`
- **관련 Entity**: TaskChecklist

---

## 3.11 댓글 작성 및 Mention (FR-COMMENT-001~003)

- **목적**: Task에 대한 논의를 남기고 특정 팀원에게 알린다.
- **사용자**: MEMBER 이상, GUEST(제한적)
- **사전 조건**: Task 존재
- **입력값**: content (`@username` 포함 가능)
- **처리 과정**:
  1. TaskComment 저장
  2. content 내 `@username` 파싱하여 대상 사용자 식별
  3. 대상자에게 Mention Notification 생성 및 SSE 전송
  4. ActivityLog 기록
- **출력값**: 생성된 댓글 정보
- **예외 상황**: 존재하지 않는 Mention 대상(무시하고 댓글은 저장), 권한 없음(`FORBIDDEN`)
- **관련 API**: `POST /api/tasks/{taskId}/comments`
- **관련 Entity**: TaskComment, Notification

---

## 3.12 실시간 알림 수신 (FR-NOTIFICATION-002)

- **목적**: 사용자에게 이벤트 발생 즉시 알림을 전달한다.
- **사용자**: 로그인 사용자
- **사전 조건**: SSE 연결 수립
- **입력값**: 없음 (서버 → 클라이언트 Push)
- **처리 과정**: Notification 생성 이벤트를 Redis Pub/Sub으로 전파 → 해당 사용자의 SSE 커넥션으로 전송
- **출력값**: Notification 이벤트 (type, message, targetUrl)
- **예외 상황**: 연결 끊김 시 클라이언트 재연결, 미수신 알림은 REST 조회로 보완
- **관련 API**: `GET /api/notifications/subscribe` (SSE), `GET /api/notifications`
- **관련 Entity**: Notification

상세 Flow는 [10-realtime-architecture.md](./10-realtime-architecture.md) 참고.

---

## 3.13 프로젝트 채팅 (FR-CHAT-001~003)

- **목적**: 프로젝트 팀원 간 실시간 커뮤니케이션을 제공한다.
- **사용자**: 프로젝트 팀원
- **사전 조건**: WebSocket 연결 및 인증
- **입력값**: message content
- **처리 과정**: WebSocket으로 메시지 수신 → ChatMessage 저장 → 같은 프로젝트 채널 구독자에게 broadcast
- **출력값**: 실시간 메시지 이벤트, 채팅 이력 목록
- **예외 상황**: 미인증 연결 거부, 비멤버 접근 거부(`FORBIDDEN`)
- **관련 API**: `WS /ws/chat`, `GET /api/projects/{projectId}/chat/messages`
- **관련 Entity**: ChatMessage

---

## 3.14 파일 업로드 (FR-FILE-001~002)

- **목적**: 대용량 파일을 서버 부하 없이 안전하게 업로드한다.
- **사용자**: MEMBER 이상
- **사전 조건**: 없음
- **입력값**: fileName, contentType, fileSize
- **처리 과정**:
  1. Backend가 S3 Presigned URL 발급
  2. Client가 S3에 직접 PUT
  3. 업로드 완료 후 Client가 Backend에 파일 메타데이터 등록 요청
  4. ProjectFile 저장
- **출력값**: presignedUrl, 등록된 ProjectFile 정보
- **예외 상황**: 허용되지 않는 파일 형식/크기 초과(`INVALID_FILE`)
- **관련 API**: `POST /api/projects/{projectId}/files/presigned-url`, `POST /api/projects/{projectId}/files`
- **관련 Entity**: ProjectFile

상세 Flow는 [11-file-storage-design.md](./11-file-storage-design.md) 참고.

---

## 3.15 Dashboard 조회 (FR-DASHBOARD-001)

- **목적**: 프로젝트 진행 현황을 한눈에 파악한다.
- **사용자**: 프로젝트 팀원
- **사전 조건**: 없음
- **입력값**: projectId
- **처리 과정**: Redis 캐시 확인 → 없으면 Task/Member 집계 쿼리 실행 후 캐싱
- **출력값**: 전체/완료/진행중/미진행 Task 수, 진행률(%), 마감임박 Task 목록, 팀원 수, 최근 활동
- **예외 상황**: 없음 (빈 프로젝트는 0으로 응답)
- **관련 API**: `GET /api/projects/{projectId}/dashboard`
- **관련 Entity**: Task, ProjectMember, ActivityLog (Redis 캐시)

---

## 3.16 검색 (FR-SEARCH-001)

- **목적**: 프로젝트 내 Task/Document/Comment를 통합 검색한다.
- **사용자**: 프로젝트 팀원
- **사전 조건**: 없음
- **입력값**: keyword, type(optional)
- **처리 과정**: PostgreSQL `ILIKE` 또는 `tsvector` 기반 Full Text Search 실행
- **출력값**: 검색 결과 목록 (type별 그룹)
- **예외 상황**: 결과 없음(빈 배열 반환)
- **관련 API**: `GET /api/projects/{projectId}/search?keyword=`
- **관련 Entity**: Task, Document, TaskComment

---

## 3.17 프로젝트 소유권 위임 (FR-MEMBER-006)

- **목적**: OWNER가 탈퇴하거나 역할을 넘기고자 할 때 프로젝트 소유권을 다른 팀원에게 넘긴다.
- **사용자**: OWNER
- **사전 조건**: 위임 대상이 해당 프로젝트의 ProjectMember (OWNER 본인 제외)
- **입력값**: 위임 대상 memberId
- **처리 과정**:
  1. 요청자가 OWNER인지 검증
  2. 대상 ProjectMember 존재 검증
  3. 하나의 트랜잭션에서 대상 Role을 OWNER로, 기존 OWNER의 Role을 ADMIN으로 변경
  4. Project.owner_id를 대상 사용자로 갱신
  5. ActivityLog 기록, 대상자에게 Notification 생성
- **출력값**: 변경된 두 ProjectMember 정보
- **예외 상황**: 권한 없음(`FORBIDDEN`), 대상이 멤버가 아님(`MEMBER_NOT_FOUND`)
- **관련 API**: `PATCH /api/projects/{projectId}/members/{memberId}/transfer-ownership`
- **관련 Entity**: Project, ProjectMember, ActivityLog, Notification
