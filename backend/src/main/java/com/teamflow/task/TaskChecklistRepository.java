package com.teamflow.task;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskChecklistRepository extends JpaRepository<TaskChecklist, Long> {

    List<TaskChecklist> findByTaskIdOrderBySortOrder(Long taskId);

    int countByTaskId(Long taskId);

    int countByTaskIdAndDone(Long taskId, boolean done);
}
