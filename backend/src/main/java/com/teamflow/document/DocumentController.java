package com.teamflow.document;

import com.teamflow.auth.UserPrincipal;
import com.teamflow.common.dto.PageResponse;
import com.teamflow.document.dto.DocumentCreateRequest;
import com.teamflow.document.dto.DocumentResponse;
import com.teamflow.document.dto.DocumentSummaryResponse;
import com.teamflow.document.dto.DocumentUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 08-api-specification.md §8 Document. */
@RestController
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/api/projects/{projectId}/documents")
    public ResponseEntity<DocumentResponse> create(
            @PathVariable Long projectId, @AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody DocumentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentService.create(projectId, principal.userId(), request));
    }

    @GetMapping("/api/projects/{projectId}/documents")
    public PageResponse<DocumentSummaryResponse> list(
            @PathVariable Long projectId, @AuthenticationPrincipal UserPrincipal principal, @PageableDefault(size = 20) Pageable pageable) {
        return documentService.list(projectId, principal.userId(), pageable);
    }

    @GetMapping("/api/projects/{projectId}/documents/{documentId}")
    public DocumentResponse get(
            @PathVariable Long projectId, @PathVariable Long documentId, @AuthenticationPrincipal UserPrincipal principal) {
        return documentService.get(projectId, documentId, principal.userId());
    }

    @PatchMapping("/api/projects/{projectId}/documents/{documentId}")
    public DocumentResponse update(
            @PathVariable Long projectId, @PathVariable Long documentId, @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody DocumentUpdateRequest request) {
        return documentService.update(projectId, documentId, principal.userId(), request);
    }

    @DeleteMapping("/api/projects/{projectId}/documents/{documentId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long projectId, @PathVariable Long documentId, @AuthenticationPrincipal UserPrincipal principal) {
        documentService.delete(projectId, documentId, principal.userId());
        return ResponseEntity.noContent().build();
    }
}
