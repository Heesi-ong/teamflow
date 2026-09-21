package com.teamflow.notification;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 10-realtime-architecture.md §1.1: "유휴 연결 유지를 위해 주기적으로 heartbeat(comment 이벤트)를 전송". */
@Component
public class NotificationHeartbeatScheduler {

    private final SseEmitterRegistry sseEmitterRegistry;

    public NotificationHeartbeatScheduler(SseEmitterRegistry sseEmitterRegistry) {
        this.sseEmitterRegistry = sseEmitterRegistry;
    }

    @Scheduled(fixedRate = 20_000)
    public void heartbeat() {
        sseEmitterRegistry.heartbeatAll();
    }
}
