package com.teamflow.user;

public record UserResponse(Long id, String email, String name, String profileImageUrl) {

    static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getProfileImageUrl());
    }
}
