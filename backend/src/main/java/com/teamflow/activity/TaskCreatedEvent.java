package com.teamflow.activity;

/** Published by the task module (05-backend-architecture.md §3.1) — kept free of task's own types so activity never depends on task. */
public record TaskCreatedEvent(Long projectId, Long actorId, Long taskId, String taskTitle) {
}
