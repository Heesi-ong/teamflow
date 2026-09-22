package com.teamflow.activity;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    Page<ActivityLog> findByProjectIdOrderByCreatedAtDesc(Long projectId, Pageable pageable);

    List<ActivityLog> findTop10ByProjectIdOrderByCreatedAtDesc(Long projectId);
}
