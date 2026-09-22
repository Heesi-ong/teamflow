package com.teamflow.activity;

/** 07-database-design.md activity_logs.action_type. Extend as later phases add more event types. */
public enum ActivityActionType {
    PROJECT_CREATED,
    PROJECT_UPDATED,
    PROJECT_DELETED,
    TASK_CREATED,
    TASK_UPDATED,
    TASK_STATUS_CHANGED,
    TASK_ASSIGNEE_CHANGED,
    TASK_DELETED,
    COMMENT_ADDED,
    MEMBER_INVITED,
    MEMBER_JOINED,
    MEMBER_ROLE_CHANGED,
    MEMBER_REMOVED,
    MEMBER_LEFT,
    OWNERSHIP_TRANSFERRED,
    DOCUMENT_CREATED,
    DOCUMENT_UPDATED,
    DOCUMENT_DELETED
}
