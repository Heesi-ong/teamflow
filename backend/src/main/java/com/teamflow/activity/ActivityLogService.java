package com.teamflow.activity;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    public ActivityLogService(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    // REQUIRES_NEW: callers include @TransactionalEventListener(phase = AFTER_COMMIT) methods,
    // where the original transaction is still finishing its own cleanup — REQUIRED would silently
    // attach to that departing transaction instead of opening a real one, and the insert is lost.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(ActivityActionType actionType, Long projectId, Long actorId, String description) {
        activityLogRepository.save(new ActivityLog(projectId, actorId, actionType, description));
    }
}
