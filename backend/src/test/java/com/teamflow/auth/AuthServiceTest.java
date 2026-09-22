package com.teamflow.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.teamflow.auth.dto.LoginRequest;
import com.teamflow.auth.dto.SignupRequest;
import com.teamflow.auth.dto.SignupResponse;
import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorCode;
import com.teamflow.user.User;
import com.teamflow.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** 15-test-strategy.md §2 Unit Test: Service 로직의 분기/예외 검증, Repository는 Mock으로 대체. */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private RefreshTokenService refreshTokenService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AuthService newService() {
        return new AuthService(userRepository, passwordEncoder, jwtTokenProvider, refreshTokenService);
    }

    @Test
    void signup_duplicateEmail_throwsEmailAlreadyExists() {
        when(userRepository.existsByEmail("owner@teamflow.dev")).thenReturn(true);
        AuthService service = newService();

        assertThatThrownBy(() -> service.signup(new SignupRequest("owner@teamflow.dev", "password123", "Owner")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);
    }

    @Test
    void signup_passwordWithoutDigit_throwsInvalidPasswordFormat() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        AuthService service = newService();

        // 03-functional-specification.md §3.1: 8자 이상 + 영문/숫자 혼합 필수 — 숫자가 빠지면 거부되어야 한다.
        assertThatThrownBy(() -> service.signup(new SignupRequest("new@teamflow.dev", "onlyletters", "New")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PASSWORD_FORMAT);
    }

    @Test
    void signup_validRequest_savesEncodedPasswordAndReturnsResponse() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        AuthService service = newService();

        SignupResponse response = service.signup(new SignupRequest("new@teamflow.dev", "password123", "New"));

        assertThat(response.email()).isEqualTo("new@teamflow.dev");
        assertThat(response.name()).isEqualTo("New");
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        User user = new User("owner@teamflow.dev", passwordEncoder.encode("correctPass1"), "Owner");
        when(userRepository.findByEmail("owner@teamflow.dev")).thenReturn(Optional.of(user));
        AuthService service = newService();

        assertThatThrownBy(() -> service.login(new LoginRequest("owner@teamflow.dev", "wrongPass1")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void login_unknownEmail_throwsInvalidCredentials() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        AuthService service = newService();

        assertThatThrownBy(() -> service.login(new LoginRequest("nobody@teamflow.dev", "password123")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }
}
