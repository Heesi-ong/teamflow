package com.teamflow.dashboard;

import com.teamflow.auth.UserPrincipal;
import com.teamflow.dashboard.dto.DashboardResponse;
import com.teamflow.dashboard.dto.SearchResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 08-api-specification.md §10 Dashboard/Search. */
@RestController
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/api/projects/{projectId}/dashboard")
    public DashboardResponse dashboard(@PathVariable Long projectId, @AuthenticationPrincipal UserPrincipal principal) {
        return dashboardService.getDashboard(projectId, principal.userId());
    }

    @GetMapping("/api/projects/{projectId}/search")
    public SearchResponse search(
            @PathVariable Long projectId, @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String keyword, @RequestParam(required = false) SearchType type) {
        return dashboardService.search(projectId, principal.userId(), keyword, type);
    }
}
