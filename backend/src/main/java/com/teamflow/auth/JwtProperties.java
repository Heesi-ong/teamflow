package com.teamflow.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "teamflow.jwt")
public class JwtProperties {

    private String secret;
    private long accessTokenExpiryMinutes = 30;
    private long refreshTokenExpiryDays = 14;
    // 17-security-design.md §5: Refresh Token Cookie는 Secure 필수. 로컬 개발(http)만 예외로 끈다.
    private boolean cookieSecure = true;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getAccessTokenExpiryMinutes() {
        return accessTokenExpiryMinutes;
    }

    public void setAccessTokenExpiryMinutes(long accessTokenExpiryMinutes) {
        this.accessTokenExpiryMinutes = accessTokenExpiryMinutes;
    }

    public long getRefreshTokenExpiryDays() {
        return refreshTokenExpiryDays;
    }

    public void setRefreshTokenExpiryDays(long refreshTokenExpiryDays) {
        this.refreshTokenExpiryDays = refreshTokenExpiryDays;
    }

    public boolean isCookieSecure() {
        return cookieSecure;
    }

    public void setCookieSecure(boolean cookieSecure) {
        this.cookieSecure = cookieSecure;
    }
}
