package com.teamflow.dashboard;

import com.teamflow.activity.ProjectActivityEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Reuses the event task/comment already publish into the activity package (05-backend-architecture.md
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
    public void onProjectActivity(ProjectActivityEvent event) {
        dashboardService.evictCache(event.projectId());
    }
}
