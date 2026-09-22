# 05. Backend Architecture

## 1. 아키텍처 스타일: Modular Monolith

하나의 Spring Boot Application 내부에서 Domain별로 Package(Module)를 분리한다. 각 Module은 자신의 Controller/Service/Repository/Entity/DTO를 가지며, 다른 Module의 내부 구현(Repository, Entity)에 직접 접근하지 않고 Service(또는 Facade) 인터페이스를 통해서만 협력한다.

```
com.teamflow
├── auth
├── user
├── project
├── member
├── task
├── comment
├── notification
├── chat
├── document
├── file
├── activity
├── dashboard
└── common
```

## 2. Module 구조 (각 Module 공통 패턴)

```
task/
├── controller/      TaskController
├── service/         TaskService, TaskAssigneeService
├── repository/      TaskRepository
├── entity/          Task, TaskAssignee, TaskChecklist
├── dto/             TaskCreateRequest, TaskResponse ...
└── exception/       TaskNotFoundException 등 (필요 시)
```

## 3. Module별 역할과 의존성

| Module | 역할 | 의존하는 Module |
|---|---|---|
| common | 공통 응답 포맷, 예외 처리(`@ControllerAdvice`), 공통 Enum, Base Entity(createdAt/updatedAt) | 없음 |
| auth | 회원가입/로그인/JWT 발급/재발급/로그아웃 | user, common |
| user | 사용자 프로필 관리 | common |
| project | 프로젝트 CRUD, 프로젝트 상태 관리 | user, member, common |
| member | ProjectMember(초대/Role/탈퇴) 관리, 권한 검증 유틸 제공 | user, project, notification, activity, common |
| task | Task/Checklist/Assignee 관리 | project, member, notification, activity, common |
| comment | Task 댓글, Mention 파싱 | task, member, notification, activity, common |
| notification | Notification 생성/조회/SSE 전송 | user, common (Redis) |
| chat | 프로젝트 채팅(WebSocket), 채팅 이력 | project, member, auth, common |
| document | 프로젝트 문서 CRUD | project, member, file, common |
| file | S3 Presigned URL 발급, 파일 메타데이터 관리 | project, member, task, common |
| activity | ActivityLog 기록/조회 | project, member, user, common |
| dashboard | 프로젝트 통계 집계(Redis 캐싱), 통합 검색 | project, member, task, document, comment, activity, common |

의존 방향은 항상 "하위 Domain → 공용 Domain"으로 흐르며, 순환 의존을 금지한다. 예: `task`는 `notification`을 호출할 수 있지만 `notification`은 `task`를 알지 못한다 (Event 기반으로 결합도를 낮춘다, 3.1 참고).

## 3.1 Module 간 통신 방식

- 같은 트랜잭션 내에서 즉시 필요한 조회(예: Task 생성 시 ProjectMember 확인)는 **Service 직접 호출**을 사용한다.
- Task 상태 변경 → ActivityLog 기록 → Notification 생성처럼 "부가 효과" 성격의 흐름은 **Spring Application Event**(`ApplicationEventPublisher`)로 발행하고, `activity`/`notification` Module이 `@EventListener` (또는 `@TransactionalEventListener(phase = AFTER_COMMIT)`)로 구독한다.
- 이 방식으로 `task` Module은 `activity`/`notification` 구현을 몰라도 되며, 향후 MSA 분리 시 이벤트 발행 지점이 메시지 브로커 연동 지점으로 자연스럽게 치환된다 — 상태: Planned(MSA 분리 대비).

```java
// task 모듈
eventPublisher.publishEvent(new TaskStatusChangedEvent(taskId, before, after, actorId));

// activity 모듈
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void on(TaskStatusChangedEvent event) { activityLogService.record(event); }
```

## 4. Layer 구조 (Module 내부)

```
Controller → Service → Repository → Entity
```

- **Controller**: HTTP 요청/응답 변환, `@Valid` 기반 입력 검증, 인증 주체(`@AuthenticationPrincipal`) 추출. 비즈니스 로직 없음.
- **Service**: 트랜잭션 경계(`@Transactional`), 권한 검증 호출, 도메인 로직, Event 발행.
- **Repository**: `JpaRepository` 기반, 복잡한 조회는 QueryDSL 또는 `@Query` 사용.
- **Entity**: JPA Entity. Setter 대신 의미 있는 도메인 메서드 사용 (예: `task.changeStatus(newStatus)`).

## 5. 공통 응답/예외 처리 (common Module)

- 공통 응답 포맷은 [18-error-handling-policy.md](./18-error-handling-policy.md) 참고.
- 전역 예외 처리는 `@RestControllerAdvice` 하나로 통일하며, 각 Module은 필요한 Custom Exception(`BusinessException` 상속)만 정의한다.

```java
public abstract class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
}
```

## 6. 권한 검증 위치

- Role 기반 접근 제어는 Controller 레벨에서 `@PreAuthorize`로 1차 필터링(로그인 여부 등)하고, 프로젝트 단위 Role(OWNER/ADMIN/MEMBER/GUEST) 검증은 `member` Module이 제공하는 `ProjectPermissionChecker`를 Service에서 호출하는 방식으로 일원화한다. 상세는 [09-authentication-authorization.md](./09-authentication-authorization.md) 참고.

## 7. 향후 MSA 분리 방향 (참고용, 상태: Planned)

현재는 도입하지 않으며, 다음은 트래픽/조직 규모가 커질 때의 참고 방향이다.

```
Auth Service        ← auth, user
Project Service      ← project, member, document, activity
Task Service         ← task, comment
Notification Service ← notification
Chat Service         ← chat
```

Module 경계를 Service 직접 호출이 아닌 Event 기반으로 유지해온 이유가 이 분리를 저비용으로 만든다.
