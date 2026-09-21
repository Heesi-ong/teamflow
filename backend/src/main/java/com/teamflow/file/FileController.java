package com.teamflow.file;

import com.teamflow.auth.UserPrincipal;
import com.teamflow.common.dto.PageResponse;
import com.teamflow.file.dto.DownloadUrlResponse;
import com.teamflow.file.dto.FileRegisterRequest;
import com.teamflow.file.dto.PresignedUploadRequest;
import com.teamflow.file.dto.PresignedUploadResponse;
import com.teamflow.file.dto.ProjectFileResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 08-api-specification.md §9 File. */
@RestController
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/api/projects/{projectId}/files/presigned-url")
    public PresignedUploadResponse presignedUrl(
            @PathVariable Long projectId, @AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody PresignedUploadRequest request) {
        return fileService.createUploadUrl(projectId, principal.userId(), request);
    }

    @PostMapping("/api/projects/{projectId}/files")
    public ResponseEntity<ProjectFileResponse> register(
            @PathVariable Long projectId, @AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody FileRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fileService.register(projectId, principal.userId(), request));
    }

    @GetMapping("/api/projects/{projectId}/files")
    public PageResponse<ProjectFileResponse> list(
            @PathVariable Long projectId, @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long taskId, @PageableDefault(size = 20) Pageable pageable) {
        return fileService.list(projectId, principal.userId(), taskId, pageable);
    }

    @GetMapping("/api/projects/{projectId}/files/{fileId}/download-url")
    public DownloadUrlResponse downloadUrl(
            @PathVariable Long projectId, @PathVariable Long fileId, @AuthenticationPrincipal UserPrincipal principal) {
        return fileService.getDownloadUrl(projectId, fileId, principal.userId());
    }

    @DeleteMapping("/api/projects/{projectId}/files/{fileId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long projectId, @PathVariable Long fileId, @AuthenticationPrincipal UserPrincipal principal) {
        fileService.delete(projectId, fileId, principal.userId());
        return ResponseEntity.noContent().build();
    }
}
