package com.teamflow.chat;

import com.teamflow.chat.dto.ChatMessageResponse;
import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorCode;
import com.teamflow.member.ProjectMemberService;
import com.teamflow.member.ProjectRole;
import com.teamflow.user.UserService;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 03-functional-specification.md §3.13, 08-api-specification.md §7. */
@Service
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ProjectMemberService projectMemberService;
    private final UserService userService;

    public ChatService(ChatMessageRepository chatMessageRepository, ProjectMemberService projectMemberService,
            UserService userService) {
        this.chatMessageRepository = chatMessageRepository;
        this.projectMemberService = projectMemberService;
        this.userService = userService;
    }

    public boolean isMember(Long projectId, Long userId) {
        return projectMemberService.isMember(projectId, userId);
    }

    @Transactional
    public ChatMessageResponse sendMessage(Long projectId, Long authorId, String content) {
        projectMemberService.requireAtLeast(projectId, authorId, ProjectRole.GUEST);
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        ChatMessage saved = chatMessageRepository.save(new ChatMessage(projectId, authorId, content));
        return ChatMessageResponse.from(saved, userService.getSummary(authorId).name());
    }

    public List<ChatMessageResponse> history(Long projectId, Long requesterId, Long before, int size) {
        projectMemberService.requireAtLeast(projectId, requesterId, ProjectRole.GUEST);
        Pageable pageable = PageRequest.of(0, size);
        List<ChatMessage> messages = before != null
                ? chatMessageRepository.findByProjectIdAndIdLessThanOrderByIdDesc(projectId, before, pageable)
                : chatMessageRepository.findByProjectIdOrderByIdDesc(projectId, pageable);
        var authorNames = userService.getSummaries(messages.stream().map(ChatMessage::getAuthorId).toList());
        return messages.stream().map(m -> ChatMessageResponse.from(m, authorNames.get(m.getAuthorId()).name())).toList();
    }
}
