# 01. Project Overview

## 프로젝트명

**TeamFlow** — 웹 기반 팀 프로젝트 협업 플랫폼

## 프로젝트 배경

소규모~중규모 팀이 하나의 프로젝트를 진행할 때 Task 관리, 일정 관리, 문서 공유, 커뮤니케이션이 서로 다른 도구(Jira, Notion, Slack, Google Drive 등)에 분산되어 있어 맥락 전환 비용이 크다. TeamFlow는 이 핵심 기능들을 하나의 서비스로 통합하여 팀이 하나의 화면에서 프로젝트를 관리할 수 있도록 한다.

## 개발 목적

- 실제 기업에서 사용하는 협업 도구 수준의 아키텍처와 기능을 개인 프로젝트로 구현하여 백엔드/프론트엔드 실무 역량을 증명한다.
- 단순 CRUD를 넘어 인증/인가, 실시간 통신, 파일 업로드, 캐싱, 모니터링, CI/CD, 자동화 테스트 등 실무에서 요구되는 기술 스택을 통합적으로 다룬다.
- 채용 포트폴리오로서, 설계 의도와 트레이드오프를 설명할 수 있는 프로젝트를 만든다.

## 해결하려는 문제

| 문제 | TeamFlow의 해결 방식 |
|---|---|
| Task 현황이 여러 도구에 흩어져 파악이 어려움 | Kanban Board + Dashboard로 프로젝트 진행 상황을 한 화면에서 확인 |
| 팀원 간 커뮤니케이션이 Task와 분리되어 맥락을 잃음 | Task 댓글, Mention, 프로젝트 채팅을 Task/프로젝트에 종속시켜 관리 |
| 권한 관리가 없어 아무나 프로젝트를 변경할 수 있음 | RBAC 기반 Role(OWNER/ADMIN/MEMBER/GUEST) 적용 |
| 변경 이력 추적이 안 됨 | ActivityLog로 주요 변경 사항을 자동 기록 |
| 알림이 실시간으로 전달되지 않음 | SSE 기반 실시간 Notification |

## 대상 사용자

- 5~30인 규모의 소규모 개발팀 / 스타트업 팀
- 사이드 프로젝트, 스터디, 해커톤 팀
- 프로젝트 단위로 협업하는 조직 내 소규모 TF(Task Force)

## 핵심 기능

- 팀 프로젝트 생성 및 관리
- 팀원 초대 및 RBAC 권한 관리
- Kanban 기반 Task 관리 (상태/우선순위/담당자/Checklist)
- Task 댓글 및 Mention
- 실시간 알림 (SSE)
- 실시간 프로젝트 채팅 (WebSocket)
- 프로젝트 일정 관리 (Calendar)
- 프로젝트 문서 관리
- 파일 공유 (AWS S3 Presigned URL)
- 프로젝트 활동 기록 (ActivityLog)
- 프로젝트 진행률 통계 (Dashboard)
- 검색 및 필터

## 기술 스택 요약

| 영역 | 기술 |
|---|---|
| Frontend | React, TypeScript, Vite, React Query, Tailwind CSS |
| Backend | Java, Spring Boot, Spring Security, Spring Data JPA |
| 인증 | JWT (Access/Refresh Token), RBAC |
| Database | PostgreSQL |
| Cache / Real-time | Redis |
| 실시간 통신 | SSE (알림), WebSocket (채팅) |
| File Storage | AWS S3 (Presigned URL) |
| Search | PostgreSQL 검색 → Elasticsearch(확장, Optional) |
| Infra | Docker, Docker Compose, Nginx, AWS EC2 |
| CI/CD | GitHub Actions |
| Monitoring | Prometheus, Grafana |
| Test | JUnit, Mockito, Testcontainers, Playwright |

상세 기술 스택 선정 이유는 [04-system-architecture.md](./04-system-architecture.md), [05-backend-architecture.md](./05-backend-architecture.md), [06-frontend-architecture.md](./06-frontend-architecture.md)를 참고한다.

## 기대 효과

- 팀 프로젝트 운영에 필요한 핵심 기능을 하나의 서비스로 경험 가능
- 실무 수준의 인증/인가, 실시간 처리, 파일 업로드 아키텍처를 구현/검증하며 기술 역량 확보
- 테스트 자동화 및 CI/CD 파이프라인 구축 경험으로 실서비스 운영 프로세스 이해
- 모니터링 체계 구축을 통해 장애 대응 및 성능 관찰 능력 확보

## 문서 안내

본 문서 세트는 TeamFlow 개발의 기준 문서(Source of Truth)이다. 전체 문서 목록과 읽는 순서는 [README.md](../README.md)를 참고한다.
