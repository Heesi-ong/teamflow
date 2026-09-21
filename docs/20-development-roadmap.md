# 20. Development Roadmap

각 Phase는 이전 Phase의 완료를 전제로 진행한다. Phase 1~4가 핵심(Core) 경로이며, Phase 6/7 일부는 Optional 기능을 포함한다.

## Phase 1. 프로젝트 초기 설정

- Backend: Spring Boot 프로젝트 생성, Modular Monolith Package 구조 스캐폴딩([05-backend-architecture.md](./05-backend-architecture.md)), PostgreSQL/Redis 연동, 공통 응답/예외 처리 구성
- Frontend: Vite + React + TypeScript 프로젝트 생성, Tailwind CSS, React Query, Directory 구조 스캐폴딩([06-frontend-architecture.md](./06-frontend-architecture.md))
- Infra: Docker Compose(dev) 구성
- **완료 조건**: Backend/Frontend가 로컬에서 기동되고, Health Check API가 정상 응답한다.

## Phase 2. Authentication

- 회원가입/로그인/로그아웃, JWT 발급/재발급, Spring Security 필터 체인 구성
- Frontend: 로그인/회원가입 페이지, Access Token 저장 및 axios interceptor
- **완료 조건**: E2E 시나리오 "회원가입 → 로그인 → 인증 필요 API 호출"이 통과한다.

## Phase 3. Project / Member

- 프로젝트 CRUD, ProjectMember/RBAC, 팀원 초대(링크/이메일)
- Frontend: 프로젝트 목록/생성/상세, 팀원 관리 화면
- **완료 조건**: OWNER가 프로젝트를 생성하고 팀원을 초대하여 참가시키는 시나리오가 통과한다.

## Phase 4. Task / Kanban

- Task CRUD, 상태/우선순위/담당자, Checklist, Kanban Board(Drag & Drop)
- ActivityLog 연동 시작 (Task 생성/상태변경 기록)
- **완료 조건**: Kanban Board에서 Task를 생성하고 Drag & Drop으로 상태를 변경하면 화면과 DB가 즉시 일치한다.

## Phase 5. Comment / Notification

- Task 댓글, Mention 파싱, Notification 생성 및 SSE 실시간 전달
- Frontend: 알림 뱃지/드롭다운, SSE 클라이언트 연동
- **완료 조건**: Mention 발생 시 대상자 화면에 실시간 알림이 표시된다.

## Phase 6. Chat

- WebSocket(STOMP) 채팅, 채팅 이력 조회
- Frontend: 프로젝트 채팅 화면
- **완료 조건**: 두 브라우저 세션 간 실시간 메시지 송수신이 확인된다.

## Phase 7. Document / File

- 프로젝트 문서 CRUD
- S3 Presigned URL 기반 파일 업로드/다운로드
- **완료 조건**: 문서를 작성하고, 파일을 업로드하여 Task에 첨부할 수 있다.

## Phase 8. Dashboard

- 프로젝트 통계 집계 API, Redis 캐싱 적용
- Frontend: Dashboard 화면, Calendar 화면
- 검색 기능(PostgreSQL 기반) 구현
- **완료 조건**: Dashboard가 실제 Task 데이터를 기준으로 정확한 통계를 표시하고, 캐시 무효화가 정상 동작한다.

## Phase 9. Test

- Unit/Integration/API/E2E Test 작성 ([15-test-strategy.md](./15-test-strategy.md))
- 핵심 시나리오 E2E 자동화
- **완료 조건**: CI에서 전체 테스트 스위트가 통과한다.

## Phase 10. Deployment / Monitoring

- GitHub Actions CI/CD 파이프라인 구성 ([14-ci-cd-design.md](./14-ci-cd-design.md))
- Docker Compose 운영 환경 배포 ([13-infrastructure-design.md](./13-infrastructure-design.md))
- Prometheus/Grafana 모니터링 구성 ([16-monitoring-design.md](./16-monitoring-design.md))
- **완료 조건**: main 브랜치 merge 시 자동 배포되고, Grafana에서 실시간 지표가 확인된다.

## 확장 로드맵 (Phase 10 이후, 상태: Optional/Planned)

- Elasticsearch 기반 검색 확장 (Optional)
- Rate Limiting 적용 (Optional)
- Refresh Token 탈취 탐지 (Planned)
- MSA 분리 검토 (Planned)
