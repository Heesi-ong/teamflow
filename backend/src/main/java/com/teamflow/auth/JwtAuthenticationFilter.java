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

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            Claims claims = jwtTokenProvider.parseClaims(header.substring("Bearer ".length()));
            if (claims != null) {
                UserPrincipal principal = new UserPrincipal(jwtTokenProvider.getUserId(claims), claims.get("email", String.class));
                var authentication = new UsernamePasswordAuthenticationToken(principal, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }
}
