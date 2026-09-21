package com.teamflow.chat;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByProjectIdOrderByIdDesc(Long projectId, Pageable pageable);

    List<ChatMessage> findByProjectIdAndIdLessThanOrderByIdDesc(Long projectId, Long before, Pageable pageable);
}
