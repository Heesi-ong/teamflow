package com.teamflow.auth;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads `Authorization: Bearer <accessToken>`, and if it is valid, puts a
 * UserPrincipal in the SecurityContext. Registered before
 * UsernamePasswordAuthenticationFilter per 09-authentication-authorization.md §1.
 * Leaves the request unauthenticated (never rejects here) so that
 * SecurityConfig's authorizeHttpRequests rules decide 401 vs permitAll.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // Keep the legacy SSE query-token fallback scoped to this endpoint for older clients.
    // The current frontend uses Authorization headers via fetch streaming, so new tokens are
    // no longer placed in URLs, browser history, proxy logs, or referrer headers.
    private static final String SSE_SUBSCRIBE_PATH = "/api/notifications/subscribe";

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null) {
            Claims claims = jwtTokenProvider.parseClaims(token);
            if (claims != null) {
                UserPrincipal principal = new UserPrincipal(jwtTokenProvider.getUserId(claims), claims.get("email", String.class));
                var authentication = new UsernamePasswordAuthenticationToken(principal, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring("Bearer ".length());
        }
        if (SSE_SUBSCRIBE_PATH.equals(request.getServletPath())) {
            return request.getParameter("token");
        }
        return null;
    }
}
