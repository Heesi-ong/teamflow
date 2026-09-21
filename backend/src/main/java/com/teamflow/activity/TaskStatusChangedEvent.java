package com.teamflow.activity;

public record TaskStatusChangedEvent(
        Long projectId, Long actorId, Long taskId, String taskTitle, String beforeStatus, String afterStatus) {
}
