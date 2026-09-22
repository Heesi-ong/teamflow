package com.teamflow.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import com.teamflow.TestcontainersConfig;
import com.teamflow.dashboard.dto.DashboardResponse;
import com.teamflow.member.ProjectMemberService;
import com.teamflow.project.Project;
import com.teamflow.project.ProjectRepository;
import com.teamflow.task.TaskService;
import com.teamflow.task.TaskStatus;
import com.teamflow.task.dto.TaskCreateRequest;
import com.teamflow.task.dto.TaskResponse;
import com.teamflow.task.dto.TaskStatusUpdateRequest;
import com.teamflow.user.User;
import com.teamflow.user.UserRepository;
import java.time.Duration;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 15-test-strategy.md §3 Integration Test 예시: "Task 상태 변경 후 cache:dashboard:{projectId} 키가
 * 삭제된다". @Transactional을 test에 붙이지 않는다 — 붙이면 테스트 자체가 하나의 트랜잭션으로 묶여
 * 커밋이 없으니 @TransactionalEventListener(AFTER_COMMIT) 캐시 무효화 리스너가 절대 발동하지 않는다.
 */
@SpringBootTest
@Import(TestcontainersConfig.class)
class DashboardCacheIntegrationTest {

    private static final String CACHE_KEY_PREFIX = "dashboard:";

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private ProjectMemberService projectMemberService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private DashboardService dashboardService;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void taskCreationAndStatusChange_evictThenRepopulateCache() {
        User owner = userRepository.save(new User("owner-" + System.nanoTime() + "@teamflow.dev", "hash", "Owner"));
        Project project = projectRepository.save(new Project("Cache Test Project", null, null, null, owner.getId()));
        projectMemberService.addOwner(project.getId(), owner.getId());
        String cacheKey = CACHE_KEY_PREFIX + project.getId();

        DashboardResponse initial = dashboardService.getDashboard(project.getId(), owner.getId());
        assertThat(initial.totalTasks()).isZero();
        assertThat(redisTemplate.hasKey(cacheKey)).as("first read caches the result").isTrue();

        TaskResponse task = taskService.create(project.getId(), owner.getId(), new TaskCreateRequest("Task A", null, null, null, null, null));
        awaitEviction(cacheKey);

        DashboardResponse afterCreate = dashboardService.getDashboard(project.getId(), owner.getId());
        assertThat(afterCreate.totalTasks()).isEqualTo(1);
        assertThat(afterCreate.todoTasks()).isEqualTo(1);
        assertThat(redisTemplate.hasKey(cacheKey)).as("re-read repopulates the cache").isTrue();

        taskService.changeStatus(project.getId(), task.id(), owner.getId(), new TaskStatusUpdateRequest(TaskStatus.DONE, 0L));
        awaitEviction(cacheKey);

        DashboardResponse afterStatusChange = dashboardService.getDashboard(project.getId(), owner.getId());
        assertThat(afterStatusChange.doneTasks()).isEqualTo(1);
        assertThat(afterStatusChange.todoTasks()).isZero();
        assertThat(afterStatusChange.progressRate()).isEqualTo(100);
    }

    /** AFTER_COMMIT 리스너는 트랜잭션 커밋 이후 비동기 타이밍이 아니라 동기적으로 실행되지만,
     *  다음 read가 evict와 우연히 경합하지 않도록 짧게 폴링한다. */
    private void awaitEviction(String cacheKey) {
        Awaitility.await().atMost(Duration.ofSeconds(2)).until(() -> Boolean.FALSE.equals(redisTemplate.hasKey(cacheKey)));
    }
}
