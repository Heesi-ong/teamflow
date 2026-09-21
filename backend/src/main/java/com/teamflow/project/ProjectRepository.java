package com.teamflow.project;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByIdAndDeletedAtIsNull(Long id);

    Page<Project> findByIdInAndDeletedAtIsNull(List<Long> ids, Pageable pageable);

    Page<Project> findByIdInAndDeletedAtIsNullAndStatus(List<Long> ids, ProjectStatus status, Pageable pageable);
}
