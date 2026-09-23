package com.teamflow.member;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select member from ProjectMember member where member.projectId = :projectId and member.userId = :userId")
    Optional<ProjectMember> findByProjectIdAndUserIdForUpdate(
            @Param("projectId") Long projectId, @Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select member from ProjectMember member where member.id = :memberId")
    Optional<ProjectMember> findByIdForUpdate(@Param("memberId") Long memberId);

    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    List<ProjectMember> findByProjectId(Long projectId);

    List<ProjectMember> findByUserId(Long userId);
}
