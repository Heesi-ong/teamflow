package com.teamflow.chat.config;

import com.teamflow.auth.config.CorsProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/** 10-realtime-architecture.md §2.1: STOMP over WebSocket, endpoint `/ws/chat`. */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;
    private final CorsProperties corsProperties;

    public WebSocketConfig(StompAuthChannelInterceptor stompAuthChannelInterceptor, CorsProperties corsProperties) {
        this.stompAuthChannelInterceptor = stompAuthChannelInterceptor;
        this.corsProperties = corsProperties;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        var endpoint = registry.addEndpoint("/ws/chat");
        if (!corsProperties.allowedOrigins().isEmpty()) {
            endpoint.setAllowedOrigins(corsProperties.allowedOrigins().toArray(String[]::new));
        }
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/user");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }
}
