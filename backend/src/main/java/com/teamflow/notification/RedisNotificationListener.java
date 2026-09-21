package com.teamflow.notification;

import tools.jackson.databind.ObjectMapper;
import com.teamflow.notification.dto.NotificationResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * Subscribes to `notification:{userId}` (10-realtime-architecture.md §1.2) and forwards
 * to any SseEmitter this instance is holding for that user. In a single-instance
 * deployment the publisher and this subscriber are the same process, but the message
 * still round-trips through Redis so the code path matches the multi-instance case.
 */
@Component
public class RedisNotificationListener implements MessageListener {

    private static final String CHANNEL_PREFIX = "notification:";

    private final SseEmitterRegistry sseEmitterRegistry;
    private final ObjectMapper objectMapper;

    public RedisNotificationListener(SseEmitterRegistry sseEmitterRegistry, ObjectMapper objectMapper) {
        this.sseEmitterRegistry = sseEmitterRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        if (!channel.startsWith(CHANNEL_PREFIX)) {
            return;
        }
        Long userId = Long.valueOf(channel.substring(CHANNEL_PREFIX.length()));
        try {
            NotificationResponse payload = objectMapper.readValue(message.getBody(), NotificationResponse.class);
            sseEmitterRegistry.sendToUser(userId, "notification", payload);
        } catch (Exception e) {
            // Malformed payload from a future/incompatible publisher — drop it, never break the listener thread.
        }
    }
}
