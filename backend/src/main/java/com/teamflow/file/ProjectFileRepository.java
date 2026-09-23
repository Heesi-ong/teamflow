package com.teamflow.file;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectFileRepository extends JpaRepository<ProjectFile, Long> {

    Page<ProjectFile> findByProjectId(Long projectId, Pageable pageable);

    Page<ProjectFile> findByProjectIdAndTaskId(Long projectId, Long taskId, Pageable pageable);

    Optional<ProjectFile> findByIdAndProjectId(Long id, Long projectId);

    List<ProjectFile> findTop5ByProjectIdOrderByCreatedAtDesc(Long projectId);
}
