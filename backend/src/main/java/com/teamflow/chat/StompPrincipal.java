package com.teamflow.chat;

import java.security.Principal;

/** Minimal Principal so `@MessageMapping` handlers can receive the authenticated userId. */
public record StompPrincipal(Long userId) implements Principal {

    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}
