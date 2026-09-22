package com.teamflow.activity.dto;

import com.teamflow.activity.ActivityActionType;
import com.teamflow.activity.ActivityLog;
import java.time.OffsetDateTime;

public record ActivityLogResponse(
        Long id, Long actorId, String actorName, ActivityActionType actionType, String description, OffsetDateTime createdAt) {

    public static ActivityLogResponse from(ActivityLog log, String actorName) {
        return new ActivityLogResponse(log.getId(), log.getActorId(), actorName, log.getActionType(), log.getDescription(), log.getCreatedAt());
    }
}
