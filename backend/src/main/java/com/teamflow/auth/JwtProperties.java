package com.teamflow.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "teamflow.jwt")
public record JwtProperties(
        String secret,
        @DefaultValue("30") long accessTokenExpiryMinutes,
        @DefaultValue("14") long refreshTokenExpiryDays,
        // 17-security-design.md §5: Refresh Token Cookie는 Secure 필수. 로컬 개발(http)만 예외로 끈다.
        @DefaultValue("true") boolean cookieSecure,
        // 프론트/백엔드가 같은 origin(nginx 뒤)이면 Strict, Render처럼 서로 다른 origin이면
        // 브라우저가 쿠키를 아예 보내지 않으므로 None(+Secure)이 필요하다.
        @DefaultValue("Strict") String cookieSameSite) {
}
