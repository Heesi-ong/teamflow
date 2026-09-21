package com.teamflow.task;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

public interface TaskRepository extends JpaRepository<Task, Long> {

    Optional<Task> findByIdAndProjectId(Long id, Long projectId);

    @Query("""
            SELECT t FROM Task t
            WHERE t.projectId = :projectId
              AND (:status IS NULL OR t.status = :status)
              AND (:priority IS NULL OR t.priority = :priority)
              AND (:keyword IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
              AND (:assigneeId IS NULL OR EXISTS (
                    SELECT 1 FROM TaskAssignee a WHERE a.taskId = t.id AND a.userId = :assigneeId))
            """)
    Page<Task> search(
            @Param("projectId") Long projectId, @Param("status") TaskStatus status, @Param("priority") TaskPriority priority,
            @Param("assigneeId") Long assigneeId, @Param("keyword") String keyword, Pageable pageable);
}
