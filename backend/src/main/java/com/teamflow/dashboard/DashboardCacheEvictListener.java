package com.teamflow.dashboard;

import com.teamflow.activity.TaskCreatedEvent;
import com.teamflow.activity.TaskDeletedEvent;
import com.teamflow.activity.TaskStatusChangedEvent;
import com.teamflow.activity.ProjectActivityEvent;
import com.teamflow.activity.CommentAddedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Reuses the events task already publishes into the activity package (05-backend-architecture.md
 * §3.1 decoupling pattern) instead of task depending on dashboard directly — that would make a
 * cycle, since dashboard already depends on task to compute stats.
 */
@Component
public class DashboardCacheEvictListener {

    private final DashboardService dashboardService;

    public DashboardCacheEvictListener(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskCreated(TaskCreatedEvent event) {
        dashboardService.evictCache(event.projectId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskStatusChanged(TaskStatusChangedEvent event) {
        dashboardService.evictCache(event.projectId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskDeleted(TaskDeletedEvent event) {
        dashboardService.evictCache(event.projectId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentAdded(CommentAddedEvent event) {
        dashboardService.evictCache(event.projectId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProjectActivity(ProjectActivityEvent event) {
        dashboardService.evictCache(event.projectId());
    }
}
