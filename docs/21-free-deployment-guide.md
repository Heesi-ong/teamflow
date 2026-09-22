# 21. Free Deployment Guide (Render + Neon + Upstash)

[13-infrastructure-design.md](./13-infrastructure-design.md)의 EC2 배포는 실제 과금이 발생한다. 카드 등록 없이
비용 없이 배포하고 싶을 때는 이 문서대로 Render(Backend/Frontend) + Neon(PostgreSQL) + Upstash(Redis)
조합을 쓴다. 계정 생성/자격증명 발급은 각자의 계정이 필요한 작업이라 본인이 직접 해야 한다 — 아래는
그 절차와, 이후 Render Blueprint([render.yaml](../render.yaml))에 채워 넣을 값이다.

## 이 배포 경로와 EC2(`prod` 프로필) 배포의 차이

- 프론트(Render Static Site)와 백엔드(Render Web Service)가 서로 다른 origin이다. nginx 하나가 같은
  origin으로 묶어주던 EC2와 달리 CORS + `SameSite=None` 쿠키 설정이 필요해서 백엔드에 `render`라는
  별도 Spring 프로필을 추가했다([application.yml](../backend/src/main/resources/application.yml)).
- **파일 업로드(S3)는 이 배포 범위 밖이다.** `render.yaml`에 S3 관련 값을 `unconfigured`라는
  placeholder로 뒀다 — AWS SDK가 빈 문자열은 기동 시점에 거부하기 때문에(`Access key ID cannot be
  blank`, 로컬에서 실제로 재현해 확인함) 앱을 정상 기동시키려면 값이 아예 없으면 안 되고 non-blank
  placeholder가 필요하다. 앱 기동에는 지장이 없고, 파일 업로드를 시도하면 그 기능만 인증 에러로 실패한다.
  나중에 필요해지면 Cloudflare R2 등 S3 호환 무료 스토리지를 붙이고 같은 환경변수(`S3_BUCKET`,
  `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `S3_REGION`)를 실제 값으로 채우면 된다.
- **Prometheus/Grafana 모니터링 스택은 이 무료 조합으로 올릴 곳이 없다.** [16-monitoring-design.md](./16-monitoring-design.md)의
  구성은 `docker-compose.prod.yml`로 로컬 검증까지만 마친 상태로 남는다.
- Render 무료 웹 서비스는 15분간 요청이 없으면 슬립하고, 다음 요청에서 30~60초 콜드 스타트가 걸린다.
  Neon도 유휴 시 자동 정지 후 첫 쿼리에서 깨어나는 데 약간의 지연이 있다. 포트폴리오 데모 용도로는
  허용 가능한 트레이드오프로 보고 진행한다.

## 1. Neon (PostgreSQL) — 카드 등록 없이 가입

1. https://neon.tech 에서 이메일 또는 GitHub 계정으로 가입한다.
2. 새 프로젝트 생성 시 데이터베이스 이름을 `teamflow`로 만든다(리전은 Render와 가까운 곳, 예:
   AWS us-east-1 계열을 고르면 지연이 적다).
3. 프로젝트 대시보드의 **Connection string**에서 **Direct connection**(Pooled connection 아님)을 선택한다.
   - Hikari가 자체적으로 커넥션 풀을 관리하므로 PgBouncer Pooled(Transaction mode) 연결을 쓰면
     Hibernate의 Prepared Statement와 충돌할 수 있다. 반드시 **Direct**를 쓴다.
   - 형태 예: `postgresql://<user>:<password>@<host>/teamflow?sslmode=require`
4. 아래처럼 세 값으로 분리해 Render Blueprint 설정 시 입력한다.
   - `DB_URL` = `jdbc:postgresql://<host>/teamflow?sslmode=require` (앞에 `jdbc:` 를 붙이고 user/password는 뺀다)
   - `DB_USERNAME` = `<user>`
   - `DB_PASSWORD` = `<password>`

## 2. Upstash (Redis) — 카드 등록 없이 가입

1. https://upstash.com 에서 이메일 또는 GitHub 계정으로 가입한다.
2. 새 Redis 데이터베이스를 만든다(Type: Regional, 무료 티어면 충분 — 256MB/월 500K commands).
   Render와 같은 리전(예: us-east-1)을 고르면 지연이 적다.
3. 데이터베이스 상세 페이지의 **Details** 탭에서 값을 확인한다.
   - `REDIS_HOST` = Endpoint (예: `xxx-xxx-12345.upstash.io`)
   - `REDIS_PASSWORD` = Password
   - 포트는 기본 `6379`(TLS)로, `render.yaml`에 이미 고정값으로 넣어뒀다.
4. Upstash는 무료 티어에서도 TLS가 기본이고 Pub/Sub도 지원한다 — 이 앱은 Notification을
   `notification:*` 채널로 Pub/Sub 하므로([NotificationRedisConfig](../backend/src/main/java/com/teamflow/notification/config/NotificationRedisConfig.java)) 그대로 쓸 수 있다.

## 3. JWT_SECRET 발급

로컬 개발 키를 재사용하지 말고 새로 발급한다.

```bash
openssl rand -base64 48
```

## 4. Render — 카드 등록 없이 가입, Blueprint로 배포

1. https://render.com 에서 GitHub 계정으로 가입한다(Free 플랜은 카드 불필요).
2. 이 저장소를 GitHub에 push한 상태여야 한다([render.yaml](../render.yaml)이 저장소 루트에 있어야 Render가 인식한다).
3. Render 대시보드에서 **New +** → **Blueprint** → 이 GitHub 저장소를 연결한다.
4. Render가 `render.yaml`을 읽어 `teamflow-backend`(Docker Web Service)와 `teamflow-frontend`(Static Site)
   두 서비스를 제안한다. **Apply**를 누르기 전에 `sync: false`로 표시된 환경변수 입력창이 뜬다:
   - `teamflow-backend`: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`(Neon), `REDIS_HOST`, `REDIS_PASSWORD`(Upstash),
     `JWT_SECRET`(3단계), `CORS_ALLOWED_ORIGINS` — 아직 프론트엔드 URL을 모르니 임시로 아무 값이나
     넣고 5단계에서 다시 채운다.
   - `teamflow-frontend`: `VITE_API_BASE_URL` — 아직 백엔드 URL을 모르니 임시값을 넣는다.
5. 두 서비스가 배포되면 각각의 URL이 대시보드에 표시된다(기본적으로
   `https://teamflow-backend.onrender.com`, `https://teamflow-frontend.onrender.com` 형태이며,
   이름이 이미 사용 중이면 Render가 다른 이름을 붙인다 — 실제 URL을 반드시 확인한다).
6. 실제 URL을 확인했으면 다시 채운다.
   - `teamflow-backend`의 `CORS_ALLOWED_ORIGINS` = 프론트엔드 URL(예: `https://teamflow-frontend.onrender.com`)
   - `teamflow-frontend`의 `VITE_API_BASE_URL` = 백엔드 URL(예: `https://teamflow-backend.onrender.com`)
   - 백엔드는 환경변수 변경 시 자동 재시작된다. **프론트엔드는 빌드 시점에 값을 번들에 굽기 때문에
     환경변수만 바꿔서는 반영되지 않는다** — 대시보드에서 **Manual Deploy**로 다시 빌드해야 한다.
7. 백엔드의 `/actuator/health`가 `{"status":"UP"}`을 반환하고, 프론트엔드 URL에서 회원가입 →
   로그인 → 프로젝트 생성까지 동작하면 배포 완료다.

## 5. 확인 체크리스트

- [ ] 프론트엔드 URL 접속 시 로그인 화면이 뜬다
- [ ] 회원가입 → 로그인 → `/dashboard` 진입까지 동작한다(쿠키가 cross-origin으로 정상 전달되는지 확인)
- [ ] 프로젝트 생성, Task 생성/상태변경이 동작한다(Neon 연결 확인)
- [ ] Task에 댓글을 달면 알림이 실시간으로 뜬다(Upstash Pub/Sub + SSE 확인)
- [ ] 채팅 페이지에서 메시지 송수신이 된다(WebSocket cross-origin 확인)
