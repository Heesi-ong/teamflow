package com.teamflow.chat.config;

import com.teamflow.auth.JwtTokenProvider;
import com.teamflow.chat.ChatService;
import com.teamflow.chat.StompPrincipal;
import io.jsonwebtoken.Claims;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * 10-realtime-architecture.md §2.1/§2.4: CONNECT 프레임의 Authorization 헤더로 인증하고,
 * SUBSCRIBE 시점에 대상 프로젝트의 멤버인지 검증한다. Spring Security의 HTTP Filter Chain은
 * WebSocket 핸드셰이크 이후의 STOMP 프레임을 보지 못하므로 이 인터셉터가 그 역할을 대신한다.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Pattern PROJECT_TOPIC_PATTERN = Pattern.compile("^/topic/projects/(\\d+)/chat$");
    private static final Pattern PROJECT_SEND_PATTERN = Pattern.compile("^/app/projects/(\\d+)/chat\\.send$");

    private final JwtTokenProvider jwtTokenProvider;
    private final ChatService chatService;

    public StompAuthChannelInterceptor(JwtTokenProvider jwtTokenProvider, ChatService chatService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.chatService = chatService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticate(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        } else if (StompCommand.SEND.equals(accessor.getCommand())) {
            authorizeSend(accessor);
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new MessagingException("Missing Authorization header on STOMP CONNECT");
        }
        Claims claims = jwtTokenProvider.parseClaims(header.substring("Bearer ".length()));
        if (claims == null) {
            throw new MessagingException("Invalid access token on STOMP CONNECT");
        }
        accessor.setUser(new StompPrincipal(jwtTokenProvider.getUserId(claims)));
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }
        Matcher matcher = PROJECT_TOPIC_PATTERN.matcher(destination);
        if (!matcher.matches()) {
            return;
        }
        Long projectId = Long.valueOf(matcher.group(1));
        StompPrincipal principal = (StompPrincipal) accessor.getUser();
        if (principal == null || !chatService.isMember(projectId, principal.userId())) {
            throw new MessagingException("Not a member of project " + projectId);
        }
    }

    private void authorizeSend(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        Matcher matcher = destination == null ? null : PROJECT_SEND_PATTERN.matcher(destination);
        if (matcher == null || !matcher.matches()) {
            // Simple Broker가 /topic, /user를 직접 처리하므로 클라이언트가 broker 목적지로
            // 발행하면 ChatWebSocketController의 멤버십 검증을 우회할 수 있다.
            throw new MessagingException("Only application chat destinations may be sent");
        }
        StompPrincipal principal = (StompPrincipal) accessor.getUser();
        Long projectId = Long.valueOf(matcher.group(1));
        if (principal == null || !chatService.isMember(projectId, principal.userId())) {
            throw new MessagingException("Not a member of project " + projectId);
        }
    }
}
