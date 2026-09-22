package com.teamflow.chat;

import com.teamflow.auth.UserPrincipal;
import com.teamflow.chat.dto.ChatMessageResponse;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** 08-api-specification.md §7 Chat (REST 이력 조회). */
@RestController
@Validated
public class ChatMessageController {

    private final ChatService chatService;

    public ChatMessageController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/api/projects/{projectId}/chat/messages")
    public List<ChatMessageResponse> history(
            @PathVariable Long projectId, @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long before,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return chatService.history(projectId, principal.userId(), before, size);
    }
}
