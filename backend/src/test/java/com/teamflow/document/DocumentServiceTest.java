package com.teamflow.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorCode;
import com.teamflow.document.dto.DocumentUpdateRequest;
import com.teamflow.member.ProjectMember;
import com.teamflow.member.ProjectMemberService;
import com.teamflow.member.ProjectRole;
import com.teamflow.user.UserService;
import com.teamflow.user.UserSummary;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 15-test-strategy.md §2 Unit Test. */
@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private ProjectMemberService projectMemberService;
    @Mock
    private UserService userService;

    private DocumentService newService() {
        return new DocumentService(documentRepository, projectMemberService, userService);
    }

    @Test
    void update_withBlankTitle_throwsInvalidRequest() {
        // title은 부분 업데이트라 null(=변경 안 함)은 허용하지만, 빈 문자열로 지우는 시도는 막아야 한다.
        Document document = new Document(1L, 1L, "Original", "content");
        when(documentRepository.findByIdAndProjectId(10L, 1L)).thenReturn(Optional.of(document));
        when(projectMemberService.requireAtLeast(eq(1L), eq(1L), any())).thenReturn(new ProjectMember(1L, 1L, ProjectRole.OWNER));
        DocumentService service = newService();

        assertThatThrownBy(() -> service.update(1L, 10L, 1L, new DocumentUpdateRequest(" ", null)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    void update_withNullTitle_keepsExistingTitleUnchanged() {
        Document document = new Document(1L, 1L, "Original", "content");
        when(documentRepository.findByIdAndProjectId(10L, 1L)).thenReturn(Optional.of(document));
        when(projectMemberService.requireAtLeast(eq(1L), eq(1L), any())).thenReturn(new ProjectMember(1L, 1L, ProjectRole.OWNER));
        when(userService.getSummary(1L)).thenReturn(new UserSummary(1L, "author@teamflow.dev", "Author"));
        DocumentService service = newService();

        service.update(1L, 10L, 1L, new DocumentUpdateRequest(null, "New content"));

        assertThat(document.getTitle()).isEqualTo("Original");
        assertThat(document.getContent()).isEqualTo("New content");
    }
}
