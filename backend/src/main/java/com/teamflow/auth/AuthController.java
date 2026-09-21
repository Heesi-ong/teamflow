package com.teamflow.auth;

import com.teamflow.auth.dto.LoginRequest;
import com.teamflow.auth.dto.SignupRequest;
import com.teamflow.auth.dto.SignupResponse;
import com.teamflow.auth.dto.TokenResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** 08-api-specification.md §1 Auth. */
@RestController
public class AuthController {

    private static final String REFRESH_COOKIE = "refreshToken";

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    public AuthController(AuthService authService, JwtProperties jwtProperties) {
        this.authService = authService;
        this.jwtProperties = jwtProperties;
    }

    @PostMapping("/api/auth/signup")
    public ResponseEntity<SignupResponse> signup(@RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        AuthService.IssuedTokens tokens = authService.login(request);
        setRefreshCookie(response, tokens.refreshToken());
        return ResponseEntity.ok(tokens.body());
    }

    @PostMapping("/api/auth/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken, HttpServletResponse response) {
        if (refreshToken == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        AuthService.IssuedTokens tokens = authService.refresh(refreshToken);
        setRefreshCookie(response, tokens.refreshToken());
        return ResponseEntity.ok(tokens.body());
    }

    @PostMapping("/api/auth/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserPrincipal principal, HttpServletResponse response) {
        authService.logout(principal.userId());
        clearRefreshCookie(response);
        return ResponseEntity.noContent().build();
    }

    private void setRefreshCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(jwtProperties.isCookieSecure())
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(java.time.Duration.ofDays(14))
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_COOKIE, null);
        cookie.setHttpOnly(true);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
