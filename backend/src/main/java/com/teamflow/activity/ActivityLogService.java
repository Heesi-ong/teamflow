package com.teamflow.activity;

import com.teamflow.activity.dto.ActivityLogResponse;
import com.teamflow.common.dto.PageResponse;
import com.teamflow.member.ProjectMemberService;
import com.teamflow.member.ProjectRole;
import com.teamflow.user.UserService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final ProjectMemberService projectMemberService;
    private final UserService userService;

    public ActivityLogService(ActivityLogRepository activityLogRepository, ProjectMemberService projectMemberService,
            UserService userService) {
        this.activityLogRepository = activityLogRepository;
        this.projectMemberService = projectMemberService;
        this.userService = userService;
    }

    // REQUIRES_NEW: callers include @TransactionalEventListener(phase = AFTER_COMMIT) methods,
    // where the original transaction is still finishing its own cleanup — REQUIRED would silently
    // attach to that departing transaction instead of opening a real one, and the insert is lost.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(ActivityActionType actionType, Long projectId, Long actorId, String description) {
        activityLogRepository.save(new ActivityLog(projectId, actorId, actionType, description));
    }

    public PageResponse<ActivityLogResponse> list(Long projectId, Long requesterId, Pageable pageable) {
        projectMemberService.requireAtLeast(projectId, requesterId, ProjectRole.GUEST);
        var page = activityLogRepository.findByProjectIdOrderByCreatedAtDesc(projectId, pageable);
        Map<Long, String> actorNames = actorNames(page.getContent().stream().map(ActivityLog::getActorId).toList());
        return PageResponse.of(page.map(log -> ActivityLogResponse.from(log, actorNames.get(log.getActorId()))));
    }

    /** Internal use (dashboard) — caller already verified membership. */
    public List<ActivityLogResponse> recent(Long projectId, int limit) {
        List<ActivityLog> logs = activityLogRepository.findTop10ByProjectIdOrderByCreatedAtDesc(projectId);
        Map<Long, String> actorNames = actorNames(logs.stream().map(ActivityLog::getActorId).toList());
        return logs.stream().limit(limit).map(log -> ActivityLogResponse.from(log, actorNames.get(log.getActorId()))).toList();
    }

    private Map<Long, String> actorNames(List<Long> actorIds) {
        return userService.getSummaries(actorIds).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().name()));
    }
}
