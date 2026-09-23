package com.teamflow.document;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    Page<Document> findByProjectId(Long projectId, Pageable pageable);

    Optional<Document> findByIdAndProjectId(Long id, Long projectId);

    List<Document> findTop5ByProjectIdOrderByUpdatedAtDesc(Long projectId);

    @Query("""
            SELECT d FROM Document d
            WHERE d.projectId = :projectId
              AND (LOWER(d.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                   OR LOWER(d.content) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
            """)
    List<Document> search(@Param("projectId") Long projectId, @Param("keyword") String keyword, Pageable pageable);
}
