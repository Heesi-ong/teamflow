# 08. API Specification

Base URL: `/api`. 별도 명시가 없는 한 모든 API는 인증(Bearer Access Token)이 필요하다. 공통 에러 응답 포맷은 [18-error-handling-policy.md](./18-error-handling-policy.md) 참고. Permission 표기는 [09-authentication-authorization.md](./09-authentication-authorization.md)의 Role 정의를 따른다.

## 공통 규칙

- Naming: 리소스는 복수형 명사(`/tasks`), 하위 리소스는 경로 중첩(`/projects/{projectId}/tasks/{taskId}`)
- 상태 변경처럼 부분 갱신은 `PATCH`, 전체 갱신은 `PUT`, 생성은 `POST`, 삭제는 `DELETE`
- 목록 조회는 페이지네이션 Query Parameter(`page`, `size`) 사용, 응답은 `{ content, page, size, totalElements }`

---

## 1. Auth

### `POST /api/auth/signup`
- Description: 회원가입
- Authentication: 불필요
- Request Body: `{ email, password, name }`
- Response 201: `{ id, email, name }`
- Error: `EMAIL_ALREADY_EXISTS`(409), `INVALID_PASSWORD_FORMAT`(400)

### `POST /api/auth/login`
- Description: 로그인
- Authentication: 불필요
- Request Body: `{ email, password }`
- Response 200: `{ accessToken, accessTokenExpiresIn }` + `Set-Cookie: refreshToken=...`(HttpOnly)
- Error: `INVALID_CREDENTIALS`(401)

### `POST /api/auth/refresh`
- Description: Access Token 재발급 (Refresh Token Rotation)
- Authentication: Refresh Token (HttpOnly Cookie)
- Response 200: `{ accessToken, accessTokenExpiresIn }` + `Set-Cookie: refreshToken=...`(HttpOnly, 회전된 새 값)
- Error: `INVALID_REFRESH_TOKEN`(401)

### `POST /api/auth/logout`
- Description: 로그아웃, Refresh Token 무효화
- Authentication: 필요
- Response 204
- Error: `UNAUTHORIZED`(401)

---

## 1-1. User (Profile)

### `GET /api/users/me`
- Description: 로그인한 사용자 본인 정보 조회
- Permission: 로그인 사용자
- Response 200: `{ id, email, name, profileImageUrl }`
- Error: `UNAUTHORIZED`(401)

### `POST /api/users/me/profile-image/presigned-url`
- Description: 프로필 이미지 업로드용 Presigned URL 발급 ([11-file-storage-design.md](./11-file-storage-design.md)와 동일한 Presigned URL 방식)
- Permission: 로그인 사용자
- Request Body: `{ fileName, contentType, fileSize }`
- Response 200: `{ presignedUrl, s3Key, expiresIn }`
- Error: `INVALID_FILE`(400) — 이미지 확장자(jpg/png/webp) 및 크기(5MB) 제한 초과

### `PATCH /api/users/me/profile-image`
- Description: S3 업로드 완료 후 `users.profile_image_url` 확정
- Permission: 로그인 사용자
- Request Body: `{ s3Key }`
- Response 200: `{ profileImageUrl }`

---

## 2. Project

### `POST /api/projects`
- Description: 프로젝트 생성
- Permission: 로그인 사용자 (생성자는 자동 OWNER)
- Request Body: `{ name, description, startDate, endDate }`
- Response 201: `ProjectResponse`
- Error: `INVALID_REQUEST`(400)

### `GET /api/projects`
- Description: 내가 참여 중인 프로젝트 목록 조회
- Query Parameter: `page`, `size`, `status`(optional)
- Response 200: `Page<ProjectSummaryResponse>`

### `GET /api/projects/{projectId}`
- Description: 프로젝트 상세 조회
- Permission: 프로젝트 멤버
- Path Parameter: `projectId`
- Response 200: `ProjectResponse`
- Error: `PROJECT_NOT_FOUND`(404), `FORBIDDEN`(403)

### `PATCH /api/projects/{projectId}`
- Description: 프로젝트 정보/상태 수정
- Permission: OWNER, ADMIN
- Request Body: `{ name?, description?, status?, startDate?, endDate? }`
- Response 200: `ProjectResponse`
- Error: `FORBIDDEN`(403), `PROJECT_NOT_FOUND`(404)

### `DELETE /api/projects/{projectId}`
- Description: 프로젝트 삭제 (Soft Delete — `deleted_at` 설정, 즉시 물리 삭제하지 않음. 정책은 [07-database-design.md](./07-database-design.md) 4장 참고)
- Permission: OWNER
- Response 204
- Error: `FORBIDDEN`(403), `PROJECT_NOT_FOUND`(404)

---

## 3. Member

초대는 `invitations` 테이블([07-database-design.md](./07-database-design.md) 참고)에 저장하며, 토큰은 생성 시 1회만 응답에 노출한다.

### `POST /api/projects/{projectId}/invitations`
- Description: 팀원 초대 (email 또는 초대 링크 생성)
- Permission: OWNER, ADMIN
- Request Body: `{ email?, role? }` (email 생략 시 초대 링크 토큰 반환, role 기본값 MEMBER)
- Response 201: `{ invitationId, token, role, expiresAt }`
- Error: `FORBIDDEN`(403), `ALREADY_MEMBER`(409)

### `GET /api/projects/{projectId}/invitations`
- Description: 초대 목록 조회 (대기/만료/취소 포함, token은 노출하지 않음)
- Permission: OWNER, ADMIN
- Query Parameter: `status?`(PENDING\|ACCEPTED\|EXPIRED\|REVOKED)
- Response 200: `List<InvitationResponse>` (`{ id, email, role, status, expiresAt, createdAt }`)

### `DELETE /api/projects/{projectId}/invitations/{invitationId}`
- Description: 초대 취소 (`status=REVOKED` 전환)
- Permission: OWNER, ADMIN
- Response 204
- Error: `FORBIDDEN`(403), `INVITATION_NOT_FOUND`(404)

### `POST /api/invitations/{token}/accept`
- Description: 초대 수락 및 프로젝트 참가 (invitation의 `role`로 ProjectMember 생성, `status=ACCEPTED` 전환)
- Permission: 로그인 사용자
- Response 200: `ProjectMemberResponse`
- Error: `INVITATION_EXPIRED`(410) — 만료·취소·이미 사용된 초대 공통, `INVITATION_NOT_FOUND`(404)

### `GET /api/projects/{projectId}/members`
- Description: 프로젝트 팀원 목록 조회
- Permission: 프로젝트 멤버
- Response 200: `List<ProjectMemberResponse>`

### `PATCH /api/projects/{projectId}/members/{memberId}/role`
- Description: 팀원 Role 변경 (ADMIN/MEMBER/GUEST 간 변경만 가능. OWNER 지정은 아래 `transfer-ownership` 전용 API를 사용한다)
- Permission: OWNER
- Request Body: `{ role }` (OWNER 지정 시도 시 `INVALID_REQUEST`)
- Response 200: `ProjectMemberResponse`
- Error: `FORBIDDEN`(403), `MEMBER_NOT_FOUND`(404), `INVALID_REQUEST`(400)

### `PATCH /api/projects/{projectId}/members/{memberId}/transfer-ownership`
- Description: OWNER 권한을 다른 멤버에게 위임한다. 대상 멤버는 OWNER로, 기존 OWNER는 ADMIN으로 전환된다(트랜잭션 처리)
- Permission: OWNER
- Response 200: `List<ProjectMemberResponse>` (변경된 두 멤버)
- Error: `FORBIDDEN`(403), `MEMBER_NOT_FOUND`(404)

### `DELETE /api/projects/{projectId}/members/{memberId}`
- Description: 팀원 제거
- Permission: OWNER, ADMIN
- Response 204
- Error: `FORBIDDEN`(403)

### `DELETE /api/projects/{projectId}/members/me`
- Description: 프로젝트 탈퇴. OWNER는 탈퇴 전 위의 `transfer-ownership`으로 소유권을 먼저 위임해야 한다
- Permission: 프로젝트 멤버
- Response 204
- Error: `OWNER_CANNOT_LEAVE`(409)

---

## 4. Task

### `POST /api/projects/{projectId}/tasks`
- Description: Task 생성
- Permission: MEMBER 이상
- Request Body: `{ title, description, assigneeId?, priority?, startDate?, dueDate? }`
- Response 201: `TaskResponse`
- Error: `FORBIDDEN`(403), `MEMBER_NOT_FOUND`(404)

### `GET /api/projects/{projectId}/tasks`
- Description: Task 목록 조회 (Kanban Board / 필터)
- Query Parameter: `status?`, `assigneeId?`, `priority?`, `keyword?`, `page`, `size`
- Permission: 프로젝트 멤버
- Response 200: `Page<TaskResponse>`

### `GET /api/projects/{projectId}/tasks/{taskId}`
- Description: Task 상세 조회
- Permission: 프로젝트 멤버
- Response 200: `TaskDetailResponse` (Checklist/Comment/Assignee 포함)
- Error: `TASK_NOT_FOUND`(404)

Task를 수정하는 아래 3개 API(정보 수정/상태 변경/담당자 변경)는 모두 요청 Body에 클라이언트가 마지막으로 조회한 `version`을 포함해야 하며, 서버의 현재 `version`과 다르면 `TASK_VERSION_CONFLICT`(409)를 반환한다(동시 수정 충돌 검증, [07-database-design.md](./07-database-design.md) tasks.version 참고). 클라이언트는 409 수신 시 Task를 재조회한 뒤 재시도를 안내한다.

### `PATCH /api/projects/{projectId}/tasks/{taskId}`
- Description: Task 일반 정보 수정
- Permission: 작성자, 담당자, ADMIN 이상
- Request Body: `{ title?, description?, priority?, startDate?, dueDate?, version }`
- Response 200: `TaskResponse`
- Error: `FORBIDDEN`(403), `TASK_VERSION_CONFLICT`(409)

### `PATCH /api/projects/{projectId}/tasks/{taskId}/status`
- Description: Task 상태 변경 (Kanban Drag & Drop)
- Permission: 작성자, 담당자, ADMIN 이상
- Request Body: `{ status, version }`
- Response 200: `TaskResponse`
- Error: `INVALID_TASK_STATUS`(400), `FORBIDDEN`(403), `TASK_VERSION_CONFLICT`(409)

### `PATCH /api/projects/{projectId}/tasks/{taskId}/assignee`
- Description: 담당자 지정/변경
- Permission: 작성자, ADMIN 이상
- Request Body: `{ assigneeId, version }`
- Response 200: `TaskResponse`
- Error: `MEMBER_NOT_FOUND`(404), `TASK_VERSION_CONFLICT`(409)

### `DELETE /api/projects/{projectId}/tasks/{taskId}`
- Description: Task 삭제
- Permission: 작성자, ADMIN 이상
- Response 204

### `POST /api/tasks/{taskId}/checklists`
- Description: Checklist 항목 추가
- Permission: MEMBER 이상
- Request Body: `{ content }`
- Response 201: `TaskChecklistResponse`
- Error: `TASK_NOT_FOUND`(404), `FORBIDDEN`(403)

### `PATCH /api/tasks/{taskId}/checklists/{checklistId}`
- Description: Checklist 완료 상태/내용 수정
- Permission: MEMBER 이상
- Request Body: `{ content?, isDone? }`
- Response 200: `TaskChecklistResponse`
- Error: `TASK_NOT_FOUND`(404), `CHECKLIST_NOT_FOUND`(404), `FORBIDDEN`(403)

### `DELETE /api/tasks/{taskId}/checklists/{checklistId}`
- Description: Checklist 삭제
- Permission: MEMBER 이상
- Response 204
- Error: `TASK_NOT_FOUND`(404), `CHECKLIST_NOT_FOUND`(404), `FORBIDDEN`(403)

---

## 5. Comment

### `POST /api/tasks/{taskId}/comments`
- Description: 댓글 작성 (`@이름` Mention 지원 — 프로젝트 멤버의 `users.name`과 매칭, 별도 username 컬럼은 없음)
- Permission: MEMBER 이상, GUEST(제한적)
- Request Body: `{ content }`
- Response 201: `TaskCommentResponse`
- Error: `TASK_NOT_FOUND`(404), `FORBIDDEN`(403)

### `GET /api/tasks/{taskId}/comments`
- Description: 댓글 목록 조회
- Permission: 프로젝트 멤버
- Response 200: `Page<TaskCommentResponse>`
- Error: `TASK_NOT_FOUND`(404), `FORBIDDEN`(403)

### `DELETE /api/tasks/{taskId}/comments/{commentId}`
- Description: 댓글 삭제
- Permission: 작성자, ADMIN 이상
- Response 204
- Error: `TASK_NOT_FOUND`(404), `COMMENT_NOT_FOUND`(404), `FORBIDDEN`(403)

---

## 6. Notification

### `GET /api/notifications/subscribe`
- Description: SSE 구독 연결
- Permission: 로그인 사용자
- Response: `text/event-stream`

### `GET /api/notifications`
- Description: 알림 목록 조회
- Query Parameter: `isRead?`, `page`, `size`
- Response 200: `Page<NotificationResponse>`

### `PATCH /api/notifications/{notificationId}/read`
- Description: 알림 읽음 처리
- Response 200: `NotificationResponse`
- Error: `NOTIFICATION_NOT_FOUND`(404) — 본인 소유가 아닌 알림 포함

### `PATCH /api/notifications/read-all`
- Description: 전체 읽음 처리
- Response 204

---

## 7. Chat

### `WS /ws/chat`
- Description: 프로젝트 채팅 WebSocket 연결 (STOMP 기반, 구독 destination: `/topic/projects/{projectId}/chat`, 발행 destination: `/app/projects/{projectId}/chat.send`)
- Authentication: STOMP CONNECT 프레임의 `Authorization` 헤더로 Access Token 전달 ([10-realtime-architecture.md](./10-realtime-architecture.md) §2.1 — HTTP 핸드셰이크 자체는 인증 없이 열려 있고, 실제 인증은 CONNECT 프레임에서 이루어진다)

### `GET /api/projects/{projectId}/chat/messages`
- Description: 채팅 이력 조회
- Query Parameter: `before?`(cursor), `size`
- Permission: 프로젝트 멤버
- Response 200: `List<ChatMessageResponse>`

---

## 8. Document

### `POST /api/projects/{projectId}/documents`
- Description: 문서 생성
- Permission: MEMBER 이상
- Request Body: `{ title, content }`
- Response 201: `DocumentResponse`
- Error: `FORBIDDEN`(403)

### `GET /api/projects/{projectId}/documents`
- Description: 문서 목록 조회
- Permission: 프로젝트 멤버
- Response 200: `Page<DocumentSummaryResponse>`
- Error: `FORBIDDEN`(403)

### `GET /api/projects/{projectId}/documents/{documentId}`
- Description: 문서 상세 조회
- Permission: 프로젝트 멤버
- Response 200: `DocumentResponse`
- Error: `DOCUMENT_NOT_FOUND`(404), `FORBIDDEN`(403)

### `PATCH /api/projects/{projectId}/documents/{documentId}`
- Description: 문서 수정
- Permission: 작성자, ADMIN 이상
- Request Body: `{ title?, content? }`
- Response 200: `DocumentResponse`
- Error: `DOCUMENT_NOT_FOUND`(404), `FORBIDDEN`(403)

### `DELETE /api/projects/{projectId}/documents/{documentId}`
- Description: 문서 삭제
- Permission: 작성자, ADMIN 이상
- Response 204
- Error: `DOCUMENT_NOT_FOUND`(404), `FORBIDDEN`(403)

---

## 9. File

### `POST /api/projects/{projectId}/files/presigned-url`
- Description: S3 업로드용 Presigned URL 발급 (TTL 5분, [11-file-storage-design.md](./11-file-storage-design.md) §4)
- Permission: MEMBER 이상
- Request Body: `{ fileName, contentType, fileSize }`
- Response 200: `{ presignedUrl, s3Key, expiresIn }`
- Error: `INVALID_FILE`(400) — 허용되지 않는 확장자/크기 초과, `FORBIDDEN`(403)

### `POST /api/projects/{projectId}/files`
- Description: S3 업로드 완료 후 메타데이터 등록
- Permission: MEMBER 이상
- Request Body: `{ s3Key, fileName, fileSize, contentType, taskId? }`
- Response 201: `ProjectFileResponse`
- Error: `FORBIDDEN`(403), `TASK_NOT_FOUND`(404) — `taskId` 지정 시 해당 프로젝트에 없는 Task면 반환

### `GET /api/projects/{projectId}/files`
- Description: 프로젝트 파일 목록 조회
- Query Parameter: `taskId?`, `page`, `size`
- Permission: 프로젝트 멤버
- Response 200: `Page<ProjectFileResponse>`
- Error: `FORBIDDEN`(403)

### `GET /api/projects/{projectId}/files/{fileId}/download-url`
- Description: S3 다운로드용 Presigned URL 발급 (TTL 10분, [11-file-storage-design.md](./11-file-storage-design.md) §5 — 상태: Core)
- Permission: 프로젝트 멤버
- Response 200: `{ presignedUrl, expiresIn }`
- Error: `FILE_NOT_FOUND`(404), `FORBIDDEN`(403)

### `DELETE /api/projects/{projectId}/files/{fileId}`
- Description: 파일 삭제 (S3 객체 및 메타데이터)
- Permission: 업로더, ADMIN 이상
- Response 204
- Error: `FILE_NOT_FOUND`(404), `FORBIDDEN`(403)

---

## 10. Dashboard / Activity / Search

### `GET /api/projects/{projectId}/dashboard`
- Description: 프로젝트 대시보드 통계 조회 (Redis 캐시 적용, TTL 60초 + Task 생성/상태변경/삭제 시 즉시 무효화)
- Permission: 프로젝트 멤버
- Response 200: `{ totalTasks, doneTasks, inProgressTasks, todoTasks, progressRate, dueSoonTasks, memberCount, recentActivities }`
  - `inProgressTasks`는 IN_PROGRESS와 REVIEW를 합산한 값이다(별도 REVIEW 필드 없음)
  - `dueSoonTasks`: 마감일이 오늘부터 3일 이내이고 완료되지 않은 Task 목록
- Error: `FORBIDDEN`(403)

### `GET /api/projects/{projectId}/activities`
- Description: 활동 기록 조회
- Query Parameter: `page`, `size`
- Permission: 프로젝트 멤버
- Response 200: `Page<ActivityLogResponse>`
- Error: `FORBIDDEN`(403)

### `GET /api/projects/{projectId}/search`
- Description: Task/Document/Comment 통합 검색 (PostgreSQL `ILIKE`)
- Query Parameter: `keyword`, `type?`(TASK|DOCUMENT|COMMENT)
- Permission: 프로젝트 멤버
- Response 200: `{ tasks: [...], documents: [...], comments: [...] }` — `type` 미지정 시 세 종류 모두, 지정 시 해당 종류만 채워지고 나머지는 빈 배열
- Error: `FORBIDDEN`(403)

---

## 11. 공통 Error Response 예시

```json
{
  "code": "TASK_NOT_FOUND",
  "message": "Task를 찾을 수 없습니다.",
  "timestamp": "2026-09-21T10:00:00Z"
}
```

전체 Error Code 목록은 [18-error-handling-policy.md](./18-error-handling-policy.md) 참고.
