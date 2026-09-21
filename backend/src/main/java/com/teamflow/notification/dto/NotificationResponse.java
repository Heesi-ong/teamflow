package com.teamflow.notification.dto;

import com.teamflow.notification.Notification;
import com.teamflow.notification.NotificationType;
import java.time.OffsetDateTime;

public record NotificationResponse(
        Long id, NotificationType type, String message, String targetUrl, boolean isRead, OffsetDateTime createdAt) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getType(), notification.getMessage(),
                notification.getTargetUrl(), notification.isRead(), notification.getCreatedAt());
    }
}
