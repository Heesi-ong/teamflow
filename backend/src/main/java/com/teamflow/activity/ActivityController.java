package com.teamflow.activity;

import com.teamflow.activity.dto.ActivityLogResponse;
import com.teamflow.auth.UserPrincipal;
import com.teamflow.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** 08-api-specification.md §10 Activity. */
@RestController
public class ActivityController {

    private final ActivityLogService activityLogService;

    public ActivityController(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    @GetMapping("/api/projects/{projectId}/activities")
    public PageResponse<ActivityLogResponse> list(
            @PathVariable Long projectId, @AuthenticationPrincipal UserPrincipal principal, @PageableDefault(size = 20) Pageable pageable) {
        return activityLogService.list(projectId, principal.userId(), pageable);
    }
}
