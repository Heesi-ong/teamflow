package com.teamflow.member;

/** 09-authentication-authorization.md §6. Declared highest-to-lowest permission. */
public enum ProjectRole {
    OWNER, ADMIN, MEMBER, GUEST;

    public boolean isAtLeast(ProjectRole min) {
        return this.ordinal() <= min.ordinal();
    }
}
