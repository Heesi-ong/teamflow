package com.teamflow.auth.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * nginx 뒤(EC2) 배포는 프론트/백엔드가 같은 origin이라 CORS가 필요 없지만, Render처럼
 * 프론트/백엔드가 서로 다른 origin에 배포되는 경우를 위해 허용 origin을 외부에서 주입한다.
 */
@ConfigurationProperties(prefix = "teamflow.cors")
public record CorsProperties(@DefaultValue List<String> allowedOrigins) {
}
