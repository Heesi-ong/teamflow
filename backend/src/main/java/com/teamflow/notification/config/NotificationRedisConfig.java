package com.teamflow.notification.config;

import com.teamflow.notification.RedisNotificationListener;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * 실시간 알림 전파용 Redis Pub/Sub 리스너. autoStartup을 꺼둔다 — 기본값(true)이면
 * SmartLifecycle로 컨텍스트 리프레시 도중 동기적으로 Redis에 연결을 시도하는데, 이때 연결이
 * 실패하면(예: Upstash가 일시적으로 응답 없음) 그 예외가 애플리케이션 부팅 전체를 실패시킨다 —
 * 부가 기능인 실시간 알림 때문에 로그인조차 안 되는 셈이다. ApplicationReadyEvent 이후 별도
 * 스레드에서 시작해, 실패해도 앱은 정상 기동하고 잠시 후 재시도한다.
 */
@Configuration
public class NotificationRedisConfig {

    private static final Logger log = LoggerFactory.getLogger(NotificationRedisConfig.class);
    private static final long INITIAL_RETRY_DELAY_MS = 5_000;
    private static final long MAX_RETRY_DELAY_MS = 60_000;

    private final ScheduledExecutorService retryExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "redis-notification-listener-retry");
        thread.setDaemon(true);
        return thread;
    });

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory, RedisNotificationListener listener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listener, new PatternTopic("notification:*"));
        container.setAutoStartup(false);
        return container;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startListenerContainer(ApplicationReadyEvent event) {
        RedisMessageListenerContainer container = event.getApplicationContext().getBean(RedisMessageListenerContainer.class);
        scheduleStart(container, 0);
    }

    private void scheduleStart(RedisMessageListenerContainer container, long delayMs) {
        retryExecutor.schedule(() -> startWithRetry(container, delayMs), delayMs, TimeUnit.MILLISECONDS);
    }

    private void startWithRetry(RedisMessageListenerContainer container, long previousDelayMs) {
        long nextDelayMs = Math.min(
                Math.max(INITIAL_RETRY_DELAY_MS, previousDelayMs * 2), MAX_RETRY_DELAY_MS);
        try {
            if (!container.isRunning()) {
                container.start();
            }
            log.info("Redis Pub/Sub listener started");
        } catch (Exception e) {
            log.warn("Redis Pub/Sub listener failed to start; retrying in {} ms: {}",
                    nextDelayMs, e.getMessage());
            scheduleStart(container, nextDelayMs);
        }
    }

    @PreDestroy
    public void shutdownRetryExecutor() {
        retryExecutor.shutdownNow();
    }
}
