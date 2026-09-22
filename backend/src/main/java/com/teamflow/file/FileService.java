package com.teamflow.file;

import com.teamflow.common.dto.PageResponse;
import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorCode;
import com.teamflow.file.dto.DownloadUrlResponse;
import com.teamflow.file.dto.FileRegisterRequest;
import com.teamflow.file.dto.PresignedUploadRequest;
import com.teamflow.file.dto.PresignedUploadResponse;
import com.teamflow.file.dto.ProjectFileResponse;
import com.teamflow.member.ProjectMember;
import com.teamflow.member.ProjectMemberService;
import com.teamflow.member.ProjectRole;
import com.teamflow.task.TaskRepository;
import com.teamflow.user.UserService;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/** 03-functional-specification.md §3.14, 08-api-specification.md §9, 11-file-storage-design.md. */
@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    // 11-file-storage-design.md §4
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "zip", "txt", "md");
    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;
    private static final Duration UPLOAD_TTL = Duration.ofMinutes(5);
    private static final Duration DOWNLOAD_TTL = Duration.ofMinutes(10);

    private final ProjectFileRepository projectFileRepository;
    private final ProjectMemberService projectMemberService;
    private final TaskRepository taskRepository;
    private final UserService userService;
    private final S3Presigner s3Presigner;
    private final S3Client s3Client;
    private final S3Properties s3Properties;

    public FileService(ProjectFileRepository projectFileRepository, ProjectMemberService projectMemberService,
            TaskRepository taskRepository, UserService userService, S3Presigner s3Presigner, S3Client s3Client,
            S3Properties s3Properties) {
        this.projectFileRepository = projectFileRepository;
        this.projectMemberService = projectMemberService;
        this.taskRepository = taskRepository;
        this.userService = userService;
        this.s3Presigner = s3Presigner;
        this.s3Client = s3Client;
        this.s3Properties = s3Properties;
    }

    public PresignedUploadResponse createUploadUrl(Long projectId, Long userId, PresignedUploadRequest request) {
        projectMemberService.requireAtLeast(projectId, userId, ProjectRole.MEMBER);
        validate(request.fileName(), request.fileSize());

        String s3Key = buildKey(projectId, request.fileName());
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(s3Key)
                .contentType(request.contentType())
                .build();
        var presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(UPLOAD_TTL)
                .putObjectRequest(putObjectRequest)
                .build();
        String url = s3Presigner.presignPutObject(presignRequest).url().toString();
        return new PresignedUploadResponse(url, s3Key, UPLOAD_TTL.toSeconds());
    }

    @Transactional
    public ProjectFileResponse register(Long projectId, Long uploaderId, FileRegisterRequest request) {
        projectMemberService.requireAtLeast(projectId, uploaderId, ProjectRole.MEMBER);
        // s3Key는 createUploadUrl()이 이 프로젝트용으로 발급한 것이어야 한다 — 그렇지 않으면 다른
        // 프로젝트에서 얻은 키(예: 자신이 멤버였던 다른 프로젝트에 업로드한 파일)를 등록해 그 객체에
        // 대한 접근을 이 프로젝트로 복제해올 수 있다.
        if (!request.s3Key().startsWith(keyPrefix(projectId))) {
            throw new BusinessException(ErrorCode.FILE_KEY_MISMATCH);
        }
        if (request.taskId() != null && taskRepository.findByIdAndProjectId(request.taskId(), projectId).isEmpty()) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }
        ProjectFile file = projectFileRepository.save(new ProjectFile(
                projectId, request.taskId(), uploaderId, request.fileName(), request.s3Key(), request.fileSize(), request.contentType()));
        return ProjectFileResponse.from(file, userService.getSummary(uploaderId).name());
    }

    public PageResponse<ProjectFileResponse> list(Long projectId, Long requesterId, Long taskId, Pageable pageable) {
        projectMemberService.requireAtLeast(projectId, requesterId, ProjectRole.GUEST);
        Page<ProjectFile> page = taskId != null
                ? projectFileRepository.findByProjectIdAndTaskId(projectId, taskId, pageable)
                : projectFileRepository.findByProjectId(projectId, pageable);
        Map<Long, String> uploaderNames = userService.getSummaries(page.getContent().stream().map(ProjectFile::getUploaderId).toList())
                .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().name()));
        return PageResponse.of(page.map(f -> ProjectFileResponse.from(f, uploaderNames.get(f.getUploaderId()))));
    }

    public DownloadUrlResponse getDownloadUrl(Long projectId, Long fileId, Long requesterId) {
        projectMemberService.requireAtLeast(projectId, requesterId, ProjectRole.GUEST);
        ProjectFile file = findInProject(projectId, fileId);
        var presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(DOWNLOAD_TTL)
                .getObjectRequest(GetObjectRequest.builder().bucket(s3Properties.getBucket()).key(file.getS3Key()).build())
                .build();
        String url = s3Presigner.presignGetObject(presignRequest).url().toString();
        return new DownloadUrlResponse(url, DOWNLOAD_TTL.toSeconds());
    }

    @Transactional
    public void delete(Long projectId, Long fileId, Long requesterId) {
        ProjectFile file = findInProject(projectId, fileId);
        ProjectMember member = projectMemberService.requireAtLeast(projectId, requesterId, ProjectRole.GUEST);
        boolean isUploader = file.getUploaderId().equals(requesterId);
        if (!(isUploader || member.getRole().isAtLeast(ProjectRole.ADMIN))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(s3Properties.getBucket()).key(file.getS3Key()).build());
        } catch (S3Exception e) {
            // 11-file-storage-design.md §6: S3 삭제 실패해도 메타데이터는 삭제한다 (잔여 객체는 별도 배치로 정리 — Optional).
            log.warn("Failed to delete S3 object {} for file {}: {}", file.getS3Key(), fileId, e.getMessage());
        }
        projectFileRepository.delete(file);
    }

    private void validate(String fileName, long fileSize) {
        String extension = extensionOf(fileName);
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension) || fileSize <= 0 || fileSize > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_FILE);
        }
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 || dot == fileName.length() - 1 ? null : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String buildKey(Long projectId, String fileName) {
        OffsetDateTime now = OffsetDateTime.now();
        return "%s%04d/%02d/%s_%s".formatted(keyPrefix(projectId), now.getYear(), now.getMonthValue(), UUID.randomUUID(), fileName);
    }

    private String keyPrefix(Long projectId) {
        return "projects/%d/".formatted(projectId);
    }

    private ProjectFile findInProject(Long projectId, Long fileId) {
        return projectFileRepository.findByIdAndProjectId(fileId, projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
    }
}
