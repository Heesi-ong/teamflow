package com.teamflow.chat;

import com.teamflow.chat.dto.ChatMessageResponse;
import com.teamflow.chat.dto.ChatMessageSendRequest;
import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorResponse;
import java.security.Principal;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

/** 10-realtime-architecture.md §2.2: `/app/projects/{projectId}/chat.send` -> broadcast to `/topic/projects/{projectId}/chat`. */
@Controller
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketController(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/projects/{projectId}/chat.send")
    public void send(@DestinationVariable Long projectId, Principal principal, @Payload ChatMessageSendRequest request) {
        Long authorId = ((StompPrincipal) principal).userId();
        ChatMessageResponse response = chatService.sendMessage(projectId, authorId, request.content());
        messagingTemplate.convertAndSend("/topic/projects/" + projectId + "/chat", response);
    }

    // 10-realtime-architecture.md §2.4: 저장 실패 시 발신자에게만 에러를 보내고 broadcast하지 않는다.
    @MessageExceptionHandler(BusinessException.class)
    @SendToUser("/queue/errors")
    public ErrorResponse handleError(BusinessException ex) {
        return ErrorResponse.of(ex.getErrorCode());
    }
}
