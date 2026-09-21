package com.teamflow.common.config;

import java.time.OffsetDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "offsetDateTimeProvider")
public class JpaAuditingConfig {

    // Spring Data's default DateTimeProvider hands back a LocalDateTime, which it
    // cannot convert into BaseEntity's OffsetDateTime fields (no LocalDateTime ->
    // OffsetDateTime case in DefaultAuditableBeanWrapperFactory). Supplying the
    // target type directly skips that conversion entirely.
    @Bean
    public DateTimeProvider offsetDateTimeProvider() {
        return () -> Optional.of((TemporalAccessor) OffsetDateTime.now());
    }
}
