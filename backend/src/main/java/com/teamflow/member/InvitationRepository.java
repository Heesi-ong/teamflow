package com.teamflow.member;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    /** Serializes acceptance so one invitation token can create at most one membership. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Invitation> findByToken(String token);

    List<Invitation> findByProjectId(Long projectId);

    List<Invitation> findByProjectIdAndStatus(Long projectId, InvitationStatus status);
}
