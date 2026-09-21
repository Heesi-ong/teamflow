# 17. Security Design

## 1. Authentication / Authorization

- JWT 기반 Stateless 인증, RBAC 기반 인가. 상세는 [09-authentication-authorization.md](./09-authentication-authorization.md) 참고.
- 모든 프로젝트 하위 리소스 API는 요청자가 해당 프로젝트의 `ProjectMember`인지, Role이 요구 등급 이상인지를 Service 계층에서 검증한다 (수평 권한 상승 방지).

## 2. Password Encryption

- 비밀번호는 BCrypt(`strength=10` 이상)로 해시하여 저장한다.
- 평문 비밀번호는 로그, 응답 DTO, ActivityLog 어디에도 노출하지 않는다.

## 3. HTTPS

- Nginx에서 TLS를 종료하고, 모든 HTTP 요청은 HTTPS로 리다이렉트한다 (443 강제, 80은 리다이렉트 전용).
- 인증서는 Let's Encrypt(Certbot)를 사용한다.

## 4. CORS

- 허용 Origin은 Frontend 배포 도메인만 화이트리스트로 등록한다 (`*` 금지).
- `Access-Control-Allow-Credentials: true` 사용 시 Origin을 명시적으로 지정한다 (Refresh Token Cookie 전달을 위해 필요).

## 5. CSRF

- Access Token은 헤더(`Authorization: Bearer`)로 전달하므로 CSRF 위험이 낮다.
- Refresh Token Cookie는 `SameSite=Strict`(또는 `Lax`) + `HttpOnly` + `Secure` 속성을 적용하여 CSRF 및 XSS를 통한 탈취를 방지한다.
- 상태 변경 API(Cookie만으로 인증되는 요청이 없도록)는 반드시 Access Token 헤더를 함께 요구한다.

## 6. XSS

- Backend: 사용자 입력(Task 제목/설명, 댓글, 채팅, 문서 내용)은 저장 시 원문 그대로 저장하고, **출력 시점**에 Frontend에서 이스케이프 처리한다 (React는 기본적으로 JSX 렌더링 시 자동 이스케이프를 수행하므로 `dangerouslySetInnerHTML` 사용을 금지한다).
- 문서(Document)가 Rich Text/Markdown을 지원할 경우, 렌더링 라이브러리의 Sanitizer(예: DOMPurify)를 반드시 통과시킨다 — 상태: Optional(Markdown 지원 시 적용).

## 7. SQL Injection

- 모든 DB 접근은 Spring Data JPA / Parameter Binding을 사용하며 문자열 결합으로 쿼리를 생성하지 않는다.
- 검색 기능(Full Text Search)도 `@Query`의 Named Parameter 또는 QueryDSL을 사용하여 파라미터를 바인딩한다.

## 8. File Upload Security

- 업로드 허용 확장자/크기를 Presigned URL 발급 시점에 검증한다 ([11-file-storage-design.md](./11-file-storage-design.md) 참고).
- S3 Bucket은 Private로 유지하고, 다운로드도 Presigned GET URL로만 제공하여 객체 직접 노출을 방지한다.
- 실행 가능한 파일(exe, sh, bat 등)은 업로드 허용 목록에서 제외한다.

## 9. Rate Limiting

- 로그인, 초대 발송 등 남용 가능성이 있는 API에 Redis 기반 Rate Limiting을 적용한다 ([12-cache-redis-design.md](./12-cache-redis-design.md) 참고) — 상태: Optional.

## 10. Secret Management

- JWT Secret, DB 비밀번호, AWS 자격증명은 코드/Git에 포함하지 않고 환경변수 및 GitHub Actions Secrets로 관리한다.
- AWS 자격증명은 최소 권한 원칙에 따라 S3 특정 Bucket에 대한 PutObject/GetObject/DeleteObject 권한만 부여한 IAM 정책을 사용한다.

## 11. 기타

- API 응답에 내부 스택 트레이스, DB 에러 원문을 노출하지 않는다 ([18-error-handling-policy.md](./18-error-handling-policy.md) 참고).
- 의존성 취약점은 `./gradlew dependencyCheckAnalyze` 또는 GitHub Dependabot Alert로 주기적으로 점검한다 — 상태: Optional.
