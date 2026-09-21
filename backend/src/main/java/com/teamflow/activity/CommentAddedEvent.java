package com.teamflow.activity;

public record CommentAddedEvent(Long projectId, Long actorId, Long taskId, String taskTitle) {
}
