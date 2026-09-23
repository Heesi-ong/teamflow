package com.teamflow.document;

import com.teamflow.activity.ActivityActionType;
import com.teamflow.activity.ProjectActivityEvent;
import com.teamflow.common.dto.PageResponse;
import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorCode;
import com.teamflow.document.dto.DocumentCreateRequest;
import com.teamflow.document.dto.DocumentResponse;
import com.teamflow.document.dto.DocumentSummaryResponse;
import com.teamflow.document.dto.DocumentUpdateRequest;
import com.teamflow.member.ProjectMember;
import com.teamflow.member.ProjectMemberService;
import com.teamflow.member.ProjectRole;
import com.teamflow.user.UserService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 03-functional-specification.md (Document는 Phase 7), 08-api-specification.md §8. */
@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final ProjectMemberService projectMemberService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    public DocumentService(DocumentRepository documentRepository, ProjectMemberService projectMemberService,
            UserService userService, ApplicationEventPublisher eventPublisher) {
        this.documentRepository = documentRepository;
        this.projectMemberService = projectMemberService;
        this.userService = userService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public DocumentResponse create(Long projectId, Long authorId, DocumentCreateRequest request) {
        projectMemberService.requireAtLeast(projectId, authorId, ProjectRole.MEMBER);
        Document document = documentRepository.save(new Document(projectId, authorId, request.title(), request.content()));
        eventPublisher.publishEvent(new ProjectActivityEvent(ActivityActionType.DOCUMENT_CREATED,
                projectId, authorId, "문서 \"" + document.getTitle() + "\" 생성됨"));
        return DocumentResponse.from(document, userService.getSummary(authorId).name());
    }

    public PageResponse<DocumentSummaryResponse> list(Long projectId, Long requesterId, Pageable pageable) {
        projectMemberService.requireAtLeast(projectId, requesterId, ProjectRole.GUEST);
        Page<Document> page = documentRepository.findByProjectId(projectId, pageable);
        Map<Long, String> authorNames = authorNames(page.getContent().stream().map(Document::getAuthorId).toList());
        return PageResponse.of(page.map(d -> DocumentSummaryResponse.from(d, authorNames.get(d.getAuthorId()))));
    }

    public DocumentResponse get(Long projectId, Long documentId, Long requesterId) {
        projectMemberService.requireAtLeast(projectId, requesterId, ProjectRole.GUEST);
        Document document = findInProject(projectId, documentId);
        return DocumentResponse.from(document, userService.getSummary(document.getAuthorId()).name());
    }

    @Transactional
    public DocumentResponse update(Long projectId, Long documentId, Long requesterId, DocumentUpdateRequest request) {
        Document document = findInProject(projectId, documentId);
        requireAuthorOrAdmin(projectId, requesterId, document);
        // title은 부분 업데이트라 null(=변경 안 함)은 허용하지만, 빈 문자열로 지우는 건 막는다.
        if (request.title() != null && request.title().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        document.update(request.title(), request.content());
        eventPublisher.publishEvent(new ProjectActivityEvent(ActivityActionType.DOCUMENT_UPDATED,
                projectId, requesterId, "문서 \"" + document.getTitle() + "\" 수정됨"));
        return DocumentResponse.from(document, userService.getSummary(document.getAuthorId()).name());
    }

    @Transactional
    public void delete(Long projectId, Long documentId, Long requesterId) {
        Document document = findInProject(projectId, documentId);
        requireAuthorOrAdmin(projectId, requesterId, document);
        documentRepository.delete(document);
        eventPublisher.publishEvent(new ProjectActivityEvent(ActivityActionType.DOCUMENT_DELETED,
                projectId, requesterId, "문서 \"" + document.getTitle() + "\" 삭제됨"));
    }

    /** Internal use (dashboard) — caller already verified membership. */
    public List<DocumentSummaryResponse> recent(Long projectId) {
        List<Document> documents = documentRepository.findTop5ByProjectIdOrderByUpdatedAtDesc(projectId);
        Map<Long, String> authorNames = authorNames(documents.stream().map(Document::getAuthorId).toList());
        return documents.stream().map(d -> DocumentSummaryResponse.from(d, authorNames.get(d.getAuthorId()))).toList();
    }

    /** Internal use (dashboard search) — caller already verified membership. */
    public List<DocumentSummaryResponse> search(Long projectId, String keyword, int limit) {
        List<Document> documents = documentRepository.search(projectId, keyword, PageRequest.of(0, limit));
        Map<Long, String> authorNames = authorNames(documents.stream().map(Document::getAuthorId).toList());
        return documents.stream().map(d -> DocumentSummaryResponse.from(d, authorNames.get(d.getAuthorId()))).toList();
    }

    private void requireAuthorOrAdmin(Long projectId, Long requesterId, Document document) {
        ProjectMember member = projectMemberService.requireAtLeast(projectId, requesterId, ProjectRole.GUEST);
        boolean isAuthor = document.getAuthorId().equals(requesterId);
        if (!(isAuthor || member.getRole().isAtLeast(ProjectRole.ADMIN))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private Document findInProject(Long projectId, Long documentId) {
        return documentRepository.findByIdAndProjectId(documentId, projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND));
    }

    private Map<Long, String> authorNames(List<Long> authorIds) {
        var summaries = userService.getSummaries(authorIds);
        return summaries.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().name()));
    }
}
