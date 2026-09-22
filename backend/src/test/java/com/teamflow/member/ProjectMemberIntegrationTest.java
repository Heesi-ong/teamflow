package com.teamflow.member;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.teamflow.TestcontainersConfig;
import com.teamflow.project.Project;
import com.teamflow.project.ProjectRepository;
import com.teamflow.user.User;
import com.teamflow.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * 15-test-strategy.md §3 Integration Test 예시: "ProjectMember UNIQUE 제약으로 중복 초대 수락 시
 * 예외가 발생한다" — 실제 PostgreSQL 제약을 검증하므로 Repository를 직접 사용한다(애플리케이션 레벨의
 * ALREADY_MEMBER 사전 체크를 우회해, DB 제약 자체가 살아있는지 확인).
 */
@SpringBootTest
@Import(TestcontainersConfig.class)
class ProjectMemberIntegrationTest {

    @Autowired
    private ProjectMemberRepository projectMemberRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private UserRepository userRepository;

    @Test
    void duplicateProjectMember_violatesUniqueConstraint() {
        User owner = userRepository.save(new User("owner-" + System.nanoTime() + "@teamflow.dev", "hash", "Owner"));
        Project project = projectRepository.save(new Project("Integration Test Project", null, null, null, owner.getId()));
        projectMemberRepository.save(new ProjectMember(project.getId(), owner.getId(), ProjectRole.OWNER));
        projectMemberRepository.flush();

        assertThatThrownBy(() -> {
            projectMemberRepository.save(new ProjectMember(project.getId(), owner.getId(), ProjectRole.MEMBER));
            projectMemberRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
