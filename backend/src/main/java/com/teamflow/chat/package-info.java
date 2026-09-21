/**
 * 프로젝트 채팅(WebSocket/STOMP), 채팅 이력. 의존: project, member, auth, common.
 * (auth: STOMP CONNECT 인증은 Spring Security의 HTTP Filter Chain을 거치지 않으므로
 * ChannelInterceptor가 JwtTokenProvider를 직접 호출해야 한다 — 10-realtime-architecture.md §2.1.)
 */
package com.teamflow.chat;
