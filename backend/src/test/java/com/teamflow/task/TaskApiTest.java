package com.teamflow.task;

import static org.assertj.core.api.Assertions.assertThat;

import com.teamflow.ApiTestSupport;
import com.teamflow.TestcontainersConfig;
import com.teamflow.auth.JwtTokenProvider;
import com.teamflow.common.exception.ErrorResponse;
import com.teamflow.member.ProjectMember;
import com.teamflow.member.ProjectMemberRepository;
import com.teamflow.member.ProjectMemberService;
import com.teamflow.member.ProjectRole;
import com.teamflow.project.Project;
import com.teamflow.project.ProjectRepository;
import com.teamflow.task.dto.TaskCreateRequest;
import com.teamflow.task.dto.TaskResponse;
import com.teamflow.user.User;
import com.teamflow.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/**
 * 15-test-strategy.md §4 API Test + §6 Security Test: 문서에 나온 예시("인증 없이 POST /api/projects
 * 호출 시 401", "존재하지 않는 Task 조회 시 TASK_NOT_FOUND")와, 수평 권한 상승/Role 미만 접근 케이스.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig.class)
class TaskApiTest {

    @LocalServerPort
    private int port;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private ProjectMemberService projectMemberService;
    @Autowired
    private ProjectMemberRepository projectMemberRepository;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    // AuthApiTest와 동일 이유로 JDK HttpClient 기반 팩토리를 쓴다.
    private final RestTemplate restTemplate = new RestTemplate(new org.springframework.http.client.JdkClientHttpRequestFactory());

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private User newUser() {
        return userRepository.save(new User("user-" + System.nanoTime() + "@teamflow.dev", "hash", "User"));
    }

    private Project newProject(User owner) {
        Project project = projectRepository.save(new Project("Project " + System.nanoTime(), null, null, null, owner.getId()));
        projectMemberService.addOwner(project.getId(), owner.getId());
        return project;
    }

    private HttpEntity<Object> authed(User user, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail()));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    @Test
    void createProject_withoutAuthentication_returns401() {
        // 08-api-specification.md 공통 규칙 + 15-test-strategy.md §4 예시: 인증 없이 POST /api/projects 호출 시 401.
        HttpStatusCodeException ex = ApiTestSupport.catchStatusException(() -> restTemplate.postForEntity(
                url("/api/projects"), new HttpEntity<>(java.util.Map.of("name", "No Auth Project")), Object.class));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getTask_nonExistentId_returns404WithTaskNotFoundCode() {
        // 15-test-strategy.md §4 예시: 존재하지 않는 Task 조회 시 TASK_NOT_FOUND 코드를 반환한다.
        User owner = newUser();
        Project project = newProject(owner);

        HttpStatusCodeException ex = ApiTestSupport.catchStatusException(() -> restTemplate.exchange(
                url("/api/projects/" + project.getId() + "/tasks/999999"), HttpMethod.GET, authed(owner, null), Object.class));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getResponseBodyAs(ErrorResponse.class).code()).isEqualTo("TASK_NOT_FOUND");
    }

    @Test
    void getTask_asNonMemberOfThatProject_returns403_horizontalPrivilegeEscalationBlocked() {
        // 수평 권한 상승: URL의 projectId 자체에 대해 멤버가 아닌 사용자는 그 프로젝트의 어떤
        // Task를 조회하려 해도 403이어야 한다.
        User owner = newUser();
        Project project = newProject(owner);
        ResponseEntity<TaskResponse> created = restTemplate.exchange(
                url("/api/projects/" + project.getId() + "/tasks"), HttpMethod.POST,
                authed(owner, new TaskCreateRequest("Owner's Task", null, null, null, null, null)), TaskResponse.class);
        Long taskId = created.getBody().id();

        User stranger = newUser();

        HttpStatusCodeException ex = ApiTestSupport.catchStatusException(() -> restTemplate.exchange(
                url("/api/projects/" + project.getId() + "/tasks/" + taskId), HttpMethod.GET, authed(stranger, null), Object.class));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getTask_byMismatchedProjectId_returns404_doesNotLeakTaskExistenceAcrossProjects() {
        User ownerA = newUser();
        Project projectA = newProject(ownerA);
        ResponseEntity<TaskResponse> created = restTemplate.exchange(
                url("/api/projects/" + projectA.getId() + "/tasks"), HttpMethod.POST,
                authed(ownerA, new TaskCreateRequest("Project A's Task", null, null, null, null, null)), TaskResponse.class);
        Long taskIdInProjectA = created.getBody().id();

        User ownerB = newUser();
        Project projectB = newProject(ownerB);

        // ownerB는 projectB의 정당한 OWNER지만, projectA 소속 Task를 projectB 경로로 조회 시도하면
        // (URL의 taskId를 다른 프로젝트 값으로 바꿔치기하는 시도를 흉내) 프로젝트-scoped 조회가 그냥
        // 못 찾은 것으로 처리되어 404가 나온다 — "다른 프로젝트에 그 ID의 Task가 존재한다"는 사실 자체를
        // 흘리지 않는, 403보다 더 안전한 정상 동작이다.
        HttpStatusCodeException ex = ApiTestSupport.catchStatusException(() -> restTemplate.exchange(
                url("/api/projects/" + projectB.getId() + "/tasks/" + taskIdInProjectA), HttpMethod.GET,
                authed(ownerB, null), Object.class));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getResponseBodyAs(ErrorResponse.class).code()).isEqualTo("TASK_NOT_FOUND");
    }

    @Test
    void createTask_asGuestRole_returns403() {
        // Role 미만 사용자의 상위 기능 접근 시도: GUEST는 MEMBER 이상이 요구되는 Task 생성을 할 수 없다.
        User owner = newUser();
        Project project = newProject(owner);
        User guest = newUser();
        // 초대 플로우 전체를 태우는 대신, GUEST 멤버십 자체를 fixture로 직접 만든다.
        projectMemberRepository.save(new ProjectMember(project.getId(), guest.getId(), ProjectRole.GUEST));

        HttpStatusCodeException ex = ApiTestSupport.catchStatusException(() -> restTemplate.exchange(
                url("/api/projects/" + project.getId() + "/tasks"), HttpMethod.POST,
                authed(guest, new TaskCreateRequest("Should be blocked", null, null, null, null, null)), Object.class));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ex.getResponseBodyAs(ErrorResponse.class).code()).isEqualTo("FORBIDDEN");
    }
}
