package com.teamflow.notification;

import tools.jackson.databind.ObjectMapper;
import com.teamflow.common.dto.PageResponse;
import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorCode;
import com.teamflow.notification.dto.NotificationResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 03-functional-specification.md §3.12, 08-api-specification.md §6, 10-realtime-architecture.md. */
@Service
public class NotificationService {

    private static final String CHANNEL_PREFIX = "notification:";

    private final NotificationRepository notificationRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public NotificationService(NotificationRepository notificationRepository, StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Saves the Notification and, once (and only if) the enclosing transaction commits,
     * publishes it on `notification:{userId}` for RedisNotificationListener to relay over SSE.
     * Publishing before commit would let a subscriber see a notification for a row that a
     * later rollback erases — same class of bug fixed for ActivityLog in Phase 4.
     */
    @Transactional
    public void create(Long userId, NotificationType type, String message, String targetUrl) {
        Notification saved = notificationRepository.save(new Notification(userId, type, message, targetUrl));
        NotificationResponse payload = NotificationResponse.from(saved);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publish(userId, payload);
            }
        });
    }

    private void publish(Long userId, NotificationResponse payload) {
        try {
            redisTemplate.convertAndSend(CHANNEL_PREFIX + userId, objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            // Best-effort realtime push; the notification itself is already durably saved and
            // will still surface through GET /api/notifications on the client's next poll/reconnect.
        }
    }

    public PageResponse<NotificationResponse> list(Long userId, Boolean isRead, Pageable pageable) {
        var page = isRead != null
                ? notificationRepository.findByUserIdAndIsRead(userId, isRead, pageable)
                : notificationRepository.findByUserId(userId, pageable);
        return PageResponse.of(page.map(NotificationResponse::from));
    }

    @Transactional
    public NotificationResponse markRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
        notification.markRead();
        return NotificationResponse.from(notification);
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllRead(userId);
    }
}
