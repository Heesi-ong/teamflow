package com.teamflow.auth.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * nginx 뒤(EC2) 배포는 프론트/백엔드가 같은 origin이라 CORS가 필요 없지만, Render처럼
 * 프론트/백엔드가 서로 다른 origin에 배포되는 경우를 위해 허용 origin을 외부에서 주입한다.
 */
@Component
@ConfigurationProperties(prefix = "teamflow.cors")
public class CorsProperties {

    private List<String> allowedOrigins = List.of();

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }
}
