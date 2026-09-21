package com.teamflow.member;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    Optional<Invitation> findByToken(String token);

    List<Invitation> findByProjectId(Long projectId);

    List<Invitation> findByProjectIdAndStatus(Long projectId, InvitationStatus status);
}
