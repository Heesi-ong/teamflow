package com.teamflow.auth.dto;

/** Response for both login and refresh — refreshToken travels only via HttpOnly Cookie, never in the body. */
public record TokenResponse(String accessToken, long accessTokenExpiresIn) {
}
