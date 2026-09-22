package com.teamflow.activity;

/** A committed project-domain change that must be audited and invalidate dashboard projections. */
public record ProjectActivityEvent(
        ActivityActionType actionType, Long projectId, Long actorId, String description) {
}
