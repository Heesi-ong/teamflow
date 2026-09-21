package com.teamflow.task.dto;

import com.teamflow.task.TaskChecklist;

public record TaskChecklistResponse(Long id, String content, boolean isDone, int sortOrder) {

    public static TaskChecklistResponse from(TaskChecklist checklist) {
        return new TaskChecklistResponse(checklist.getId(), checklist.getContent(), checklist.isDone(), checklist.getSortOrder());
    }
}
