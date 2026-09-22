package com.teamflow.file;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorCode;
import com.teamflow.file.dto.FileRegisterRequest;
import com.teamflow.member.ProjectMemberService;
import com.teamflow.task.TaskRepository;
import com.teamflow.user.UserService;
import com.teamflow.user.UserSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/** 15-test-strategy.md §2 Unit Test. */
@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private ProjectFileRepository projectFileRepository;
    @Mock
    private ProjectMemberService projectMemberService;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private UserService userService;
    @Mock
    private S3Presigner s3Presigner;
    @Mock
    private S3Client s3Client;

    private FileService newService() {
        return new FileService(projectFileRepository, projectMemberService, taskRepository, userService,
                s3Presigner, s3Client, new S3Properties());
    }

    @Test
    void register_withS3KeyIssuedForAnotherProject_throwsFileKeyMismatch() {
        // 다른 프로젝트(2)용으로 발급된 키를 이 프로젝트(1)에 등록하려는 시도 — 등록되면 프로젝트 1
        // 멤버가 프로젝트 2의 S3 객체에 대한 presigned GET을 받을 수 있게 된다.
        FileRegisterRequest request = new FileRegisterRequest(
                "projects/2/2026/09/uuid_secret.pdf", "secret.pdf", 1024L, "application/pdf", null);

        assertThatThrownBy(() -> newService().register(1L, 10L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_KEY_MISMATCH);

        verify(projectFileRepository, never()).save(any());
    }

    @Test
    void register_withS3KeyIssuedForThisProject_succeeds() {
        FileRegisterRequest request = new FileRegisterRequest(
                "projects/1/2026/09/uuid_report.pdf", "report.pdf", 1024L, "application/pdf", null);
        when(projectFileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userService.getSummary(10L)).thenReturn(new UserSummary(10L, "uploader@teamflow.dev", "Uploader"));

        newService().register(1L, 10L, request);

        verify(projectFileRepository).save(argThat(file -> file.getS3Key().equals("projects/1/2026/09/uuid_report.pdf")));
    }
}
