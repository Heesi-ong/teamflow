package com.teamflow.document;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    Page<Document> findByProjectId(Long projectId, Pageable pageable);

    Optional<Document> findByIdAndProjectId(Long id, Long projectId);
}
