package com.teamflow.notification;

import com.teamflow.auth.UserPrincipal;
import com.teamflow.common.dto.PageResponse;
import com.teamflow.notification.dto.NotificationResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** 08-api-specification.md §6 Notification. */
@RestController
public class NotificationController {

    private final NotificationService notificationService;
    private final SseEmitterRegistry sseEmitterRegistry;

    public NotificationController(NotificationService notificationService, SseEmitterRegistry sseEmitterRegistry) {
        this.notificationService = notificationService;
        this.sseEmitterRegistry = sseEmitterRegistry;
    }

    @GetMapping("/api/notifications/subscribe")
    public SseEmitter subscribe(@AuthenticationPrincipal UserPrincipal principal) {
        return sseEmitterRegistry.subscribe(principal.userId());
    }

    @GetMapping("/api/notifications")
    public PageResponse<NotificationResponse> list(
            @AuthenticationPrincipal UserPrincipal principal, @RequestParam(required = false) Boolean isRead,
            @PageableDefault(size = 20) Pageable pageable) {
        return notificationService.list(principal.userId(), isRead, pageable);
    }

    @PatchMapping("/api/notifications/{notificationId}/read")
    public NotificationResponse markRead(@PathVariable Long notificationId, @AuthenticationPrincipal UserPrincipal principal) {
        return notificationService.markRead(principal.userId(), notificationId);
    }

    @PatchMapping("/api/notifications/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markAllRead(principal.userId());
        return ResponseEntity.noContent().build();
    }
}
