package com.teamflow.file;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "teamflow.s3")
public record S3Properties(
        String bucket, String region, String endpoint, String accessKey, String secretKey,
        @DefaultValue("false") boolean pathStyleAccess) {
}
