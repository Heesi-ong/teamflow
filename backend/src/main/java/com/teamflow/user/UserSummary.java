package com.teamflow.user;

public record UserSummary(Long id, String email, String name) {

    static UserSummary from(User user) {
        return new UserSummary(user.getId(), user.getEmail(), user.getName());
    }
}
