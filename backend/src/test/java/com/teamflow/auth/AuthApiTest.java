package com.teamflow.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.teamflow.ApiTestSupport;
import com.teamflow.TestcontainersConfig;
import com.teamflow.auth.dto.LoginRequest;
import com.teamflow.auth.dto.SignupRequest;
import com.teamflow.auth.dto.TokenResponse;
import com.teamflow.common.exception.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/**
 * 15-test-strategy.md §4 API Test: HTTP Status/Body/공통 예외 응답 포맷이 08/18 문서와 일치하는지
 * 실제 Servlet 컨테이너(RANDOM_PORT) + Spring Security Filter Chain까지 통째로 검증한다.
 * TestRestTemplate/@AutoConfigureMockMvc가 이 Spring Boot 4.1 조합에서 해석되지 않아(테스트
 * classpath에 없음), 순수 RestTemplate + @LocalServerPort로 동일한 것을 검증한다. RestTemplate은
 * 4xx/5xx에서 예외를 던지는 기본 동작을 그대로 쓰고 HttpStatusCodeException에서 status/body를 꺼낸다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig.class)
class AuthApiTest {

    @LocalServerPort
    private int port;

    // 기본 SimpleClientHttpRequestFactory(HttpURLConnection 기반)는 401 응답의 캐시 제어 헤더 조합에서
    // 에러 스트림 본문을 비워서 반환하는 문제가 있어, JDK HttpClient 기반으로 바꿔 회피한다.
    private final RestTemplate restTemplate = new RestTemplate(new org.springframework.http.client.JdkClientHttpRequestFactory());

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void signup_thenLogin_succeeds() {
        String email = "api-" + System.nanoTime() + "@teamflow.dev";

        ResponseEntity<Object> signupResponse = restTemplate.postForEntity(
                url("/api/auth/signup"), new SignupRequest(email, "password123", "API User"), Object.class);
        assertThat(signupResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<TokenResponse> loginResponse = restTemplate.postForEntity(
                url("/api/auth/login"), new LoginRequest(email, "password123"), TokenResponse.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody().accessToken()).isNotBlank();
        // 09-authentication-authorization.md §2: Refresh Token은 Body가 아니라 Cookie로만 전달된다.
        assertThat(loginResponse.getHeaders().get("Set-Cookie"))
                .anyMatch(cookie -> cookie.startsWith("refreshToken=") && cookie.contains("HttpOnly"));
    }

    @Test
    void signup_duplicateEmail_returns409WithEmailAlreadyExistsCode() {
        String email = "dup-" + System.nanoTime() + "@teamflow.dev";
        restTemplate.postForEntity(url("/api/auth/signup"), new SignupRequest(email, "password123", "First"), Object.class);

        HttpStatusCodeException ex = ApiTestSupport.catchStatusException(() ->
                restTemplate.postForEntity(url("/api/auth/signup"), new SignupRequest(email, "password123", "Second"), Object.class));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getResponseBodyAs(ErrorResponse.class).code()).isEqualTo("EMAIL_ALREADY_EXISTS");
    }

    @Test
    void signup_malformedEmail_returns400WithInvalidRequestCode() {
        // SignupRequest.email의 @Email 검증은 컨트롤러에 @Valid가 있어야만 작동한다.
        HttpStatusCodeException ex = ApiTestSupport.catchStatusException(() -> restTemplate.postForEntity(
                url("/api/auth/signup"), new SignupRequest("not-an-email", "password123", "User"), Object.class));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getResponseBodyAs(ErrorResponse.class).code()).isEqualTo("INVALID_REQUEST");
    }

    @Test
    void signup_blankName_returns400WithInvalidRequestCode() {
        String email = "blankname-" + System.nanoTime() + "@teamflow.dev";

        HttpStatusCodeException ex = ApiTestSupport.catchStatusException(() -> restTemplate.postForEntity(
                url("/api/auth/signup"), new SignupRequest(email, "password123", " "), Object.class));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getResponseBodyAs(ErrorResponse.class).code()).isEqualTo("INVALID_REQUEST");
    }

    @Test
    void login_wrongPassword_returns401WithInvalidCredentialsCode() {
        String email = "wrongpass-" + System.nanoTime() + "@teamflow.dev";
        restTemplate.postForEntity(url("/api/auth/signup"), new SignupRequest(email, "password123", "User"), Object.class);

        HttpStatusCodeException ex = ApiTestSupport.catchStatusException(() ->
                restTemplate.postForEntity(url("/api/auth/login"), new LoginRequest(email, "wrongPassword1"), Object.class));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(ex.getResponseBodyAs(ErrorResponse.class).code()).isEqualTo("INVALID_CREDENTIALS");
    }
}
