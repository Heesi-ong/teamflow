package com.teamflow.dashboard;

import com.teamflow.activity.ActivityLogService;
import com.teamflow.comment.TaskCommentService;
import com.teamflow.comment.dto.TaskCommentResponse;
import com.teamflow.dashboard.dto.DashboardResponse;
import com.teamflow.dashboard.dto.SearchResponse;
import com.teamflow.document.DocumentService;
import com.teamflow.document.dto.DocumentSummaryResponse;
import com.teamflow.file.FileService;
import com.teamflow.member.ProjectMemberService;
import com.teamflow.member.ProjectRole;
import com.teamflow.task.TaskService;
import com.teamflow.task.TaskStatus;
import com.teamflow.task.dto.TaskResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/** 03-functional-specification.md §3.15-3.16, 08-api-specification.md §10. */
@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);
    private static final String CACHE_KEY_PREFIX = "dashboard:";
    private static final Duration CACHE_TTL = Duration.ofSeconds(60);
    // 문서에 명시된 기준이 없어 "마감임박"을 오늘부터 3일 이내(미완료)로 정한다.
    private static final int DUE_SOON_DAYS = 3;
    private static final int SEARCH_RESULT_LIMIT = 20;

    private final ProjectMemberService projectMemberService;
    private final TaskService taskService;
    private final ActivityLogService activityLogService;
    private final DocumentService documentService;
    private final FileService fileService;
    private final TaskCommentService taskCommentService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public DashboardService(ProjectMemberService projectMemberService, TaskService taskService,
            ActivityLogService activityLogService, DocumentService documentService, FileService fileService,
            TaskCommentService taskCommentService, StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.projectMemberService = projectMemberService;
        this.taskService = taskService;
        this.activityLogService = activityLogService;
        this.documentService = documentService;
        this.fileService = fileService;
        this.taskCommentService = taskCommentService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public DashboardResponse getDashboard(Long projectId, Long requesterId) {
        projectMemberService.requireAtLeast(projectId, requesterId, ProjectRole.GUEST);
        String cacheKey = CACHE_KEY_PREFIX + projectId;
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.readValue(cached, DashboardResponse.class);
            }
        } catch (RuntimeException ex) {
            // Dashboard cache is an optimization. Redis 장애는 원본 DB 계산을 막지 않아야 한다.
            log.warn("Dashboard cache read failed for project {}", projectId);
        }
        DashboardResponse response = compute(projectId);
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(response), CACHE_TTL);
        } catch (RuntimeException ex) {
            log.warn("Dashboard cache write failed for project {}", projectId);
        }
        return response;
    }

    public void evictCache(Long projectId) {
        try {
            redisTemplate.delete(CACHE_KEY_PREFIX + projectId);
        } catch (RuntimeException ex) {
            log.warn("Dashboard cache eviction failed for project {}", projectId);
        }
    }

    public SearchResponse search(Long projectId, Long requesterId, String keyword, SearchType type) {
        projectMemberService.requireAtLeast(projectId, requesterId, ProjectRole.GUEST);
        List<TaskResponse> tasks = type == null || type == SearchType.TASK
                ? taskService.search(projectId, keyword, SEARCH_RESULT_LIMIT) : List.of();
        List<DocumentSummaryResponse> documents = type == null || type == SearchType.DOCUMENT
                ? documentService.search(projectId, keyword, SEARCH_RESULT_LIMIT) : List.of();
        List<TaskCommentResponse> comments = type == null || type == SearchType.COMMENT
                ? taskCommentService.search(projectId, keyword, SEARCH_RESULT_LIMIT) : List.of();
        return new SearchResponse(tasks, documents, comments);
    }

    private DashboardResponse compute(Long projectId) {
        Map<TaskStatus, Long> counts = taskService.countByStatus(projectId);
        long total = taskService.countAll(projectId);
        long done = counts.getOrDefault(TaskStatus.DONE, 0L);
        long todo = counts.getOrDefault(TaskStatus.TODO, 0L);
        // "진행중"은 IN_PROGRESS와 REVIEW를 합산한다 — 08-api-specification.md 응답에는 done/inProgress/todo
        // 3개 버킷만 있고 REVIEW 전용 필드가 없어, 완료되지도 대기 중이지도 않은 상태로 묶는다.
        long inProgress = total - done - todo;
        int progressRate = total == 0 ? 0 : (int) Math.round(done * 100.0 / total);
        List<TaskResponse> dueSoon = taskService.findDueSoon(projectId, DUE_SOON_DAYS);
        long memberCount = projectMemberService.countMembers(projectId);
        var recentActivities = activityLogService.recent(projectId, 10);
        var recentDocuments = documentService.recent(projectId);
        var recentFiles = fileService.recent(projectId);
        return new DashboardResponse(
                total, done, inProgress, todo, progressRate, dueSoon, memberCount, recentActivities, recentDocuments, recentFiles);
    }
}
