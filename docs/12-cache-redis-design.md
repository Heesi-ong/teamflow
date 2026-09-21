# 12. Cache / Redis Design

## 1. Redis 사용 목적

| 용도 | 설명 |
|---|---|
| Refresh Token 저장 | 로그인 세션의 Refresh Token 보관 및 즉시 무효화(로그아웃) 지원 |
| Cache | 조회 빈도가 높고 변경 빈도가 낮은 데이터 캐싱으로 DB 부하 감소 |
| Pub/Sub | SSE Notification 이벤트 브로드캐스트 |
| Rate Limiting | 로그인 시도, 초대 발송 등 API 남용 방지 (상태: Optional) |

## 2. Key Naming Convention

```
{domain}:{purpose}:{identifier}
```

| Key 패턴 | 설명 | TTL |
|---|---|---|
| `auth:refresh:{userId}` | Refresh Token 값 | 14일 |
| `cache:project:{projectId}` | 프로젝트 기본 정보 캐시 | 10분 |
| `cache:dashboard:{projectId}` | 프로젝트 Dashboard 통계 캐시 | 5분 |
| `cache:members:{projectId}` | ProjectMember 목록 캐시 | 10분 |
| `pubsub:notification:{userId}` | SSE Notification Pub/Sub 채널 | - (채널, TTL 없음) |
| `ratelimit:login:{ip}` | 로그인 시도 횟수 | 1분 |

## 3. Cache 무효화 전략

- Write-Through가 아닌 **Cache-Aside** 패턴을 사용한다: 조회 시 캐시 확인 → 없으면 DB 조회 후 캐시 저장.
- 데이터 변경(Task 생성/상태변경, 멤버 변경 등) 발생 시 관련 캐시 키를 명시적으로 삭제(Evict)한다.
  - 예: Task 상태 변경 → `cache:dashboard:{projectId}` 삭제
  - 예: 팀원 추가/제거/Role변경 → `cache:members:{projectId}` 삭제
- TTL을 짧게(5~10분) 설정하여 Evict 누락 시에도 정합성이 오래 깨지지 않도록 한다.

## 4. Pub/Sub 채널 설계

- 채널명: `pubsub:notification:{userId}`
- Publish 메시지: `{ type, message, targetUrl, createdAt }` (JSON)
- 각 서버 인스턴스는 자신이 보유한 `SseEmitter`에 해당하는 사용자 채널만 구독한다 (연결 수립 시 SUBSCRIBE, 연결 종료 시 UNSUBSCRIBE) — 단일 인스턴스에서는 전역 리스너로 단순화 가능.

## 5. Rate Limiting (상태: Optional)

- 알고리즘: Fixed Window Counter (`INCR` + `EXPIRE`)
- 대상: `POST /api/auth/login`(IP당 분당 10회), `POST /api/projects/{projectId}/invitations`(사용자당 시간당 20회)
- 초과 시 `429 TOO_MANY_REQUESTS` 응답

## 6. 캐시 대상 선정 기준

- 캐시는 다음 조건을 모두 만족할 때만 도입한다: (1) 조회가 쓰기보다 훨씬 빈번, (2) 계산/집계 비용이 있음, (3) 약간의 지연된 일관성(Eventually Consistent)이 허용됨.
- Task 목록, Comment 목록처럼 실시간성이 중요하고 쓰기가 잦은 데이터는 캐싱하지 않는다.
