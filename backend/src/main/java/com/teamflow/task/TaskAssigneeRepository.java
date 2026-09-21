package com.teamflow.task;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskAssigneeRepository extends JpaRepository<TaskAssignee, Long> {

    List<TaskAssignee> findByTaskId(Long taskId);

    List<TaskAssignee> findByTaskIdIn(List<Long> taskIds);

    boolean existsByTaskIdAndUserId(Long taskId, Long userId);

    void deleteByTaskId(Long taskId);
}
