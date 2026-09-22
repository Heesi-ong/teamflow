package com.teamflow.comment;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {

    Page<TaskComment> findByTaskId(Long taskId, Pageable pageable);

    Optional<TaskComment> findByIdAndTaskId(Long id, Long taskId);

    // TaskComment는 project_id를 직접 갖지 않아 Task와의 theta-join으로 프로젝트 범위를 건다
    // (이 코드베이스는 모듈 간 참조에 @ManyToOne 대신 평범한 FK 컬럼만 쓰는 컨벤션이라 JPQL 연관경로가 없다).
    @Query("""
            SELECT c FROM TaskComment c, com.teamflow.task.Task t
            WHERE t.id = c.taskId AND t.projectId = :projectId
              AND LOWER(c.content) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
            """)
    List<TaskComment> search(@Param("projectId") Long projectId, @Param("keyword") String keyword, Pageable pageable);
}
