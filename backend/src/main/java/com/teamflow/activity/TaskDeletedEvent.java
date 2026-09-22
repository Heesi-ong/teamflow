package com.teamflow.activity;

/** Published on Task delete so dashboard (and any future listener) can react without task depending on it. */
public record TaskDeletedEvent(Long projectId, Long taskId) {
}
