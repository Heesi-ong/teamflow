package com.teamflow.dashboard.dto;

import com.teamflow.comment.dto.TaskCommentResponse;
import com.teamflow.document.dto.DocumentSummaryResponse;
import com.teamflow.task.dto.TaskResponse;
import java.util.List;

public record SearchResponse(List<TaskResponse> tasks, List<DocumentSummaryResponse> documents, List<TaskCommentResponse> comments) {
}
