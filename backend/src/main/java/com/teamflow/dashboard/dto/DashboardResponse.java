package com.teamflow.dashboard.dto;

import com.teamflow.activity.dto.ActivityLogResponse;
import com.teamflow.document.dto.DocumentSummaryResponse;
import com.teamflow.file.dto.ProjectFileResponse;
import com.teamflow.task.dto.TaskResponse;
import java.util.List;

public record DashboardResponse(
        long totalTasks, long doneTasks, long inProgressTasks, long todoTasks, int progressRate,
        List<TaskResponse> dueSoonTasks, long memberCount, List<ActivityLogResponse> recentActivities,
        List<DocumentSummaryResponse> recentDocuments, List<ProjectFileResponse> recentFiles) {
}
