package com.teamflow.auth;

import com.teamflow.auth.dto.LoginRequest;
import com.teamflow.auth.dto.SignupRequest;
import com.teamflow.auth.dto.SignupResponse;
import com.teamflow.auth.dto.TokenResponse;
import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorCode;
import com.teamflow.user.User;
import com.teamflow.user.UserRepository;
import com.teamflow.user.EmailNormalizer;
import java.util.regex.Pattern;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements the signup/login/refresh/logout flows in
 * 03-functional-specification.md §3.1-3.4.
 */
@Service
public class AuthService {

    // 03-functional-specification.md §3.1: 8자 이상, 영문+숫자 포함
    private static final Pattern PASSWORD_FORMAT = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider, RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        String email = EmailNormalizer.normalize(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (!PASSWORD_FORMAT.matcher(request.password()).matches()) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD_FORMAT);
        }
        User user = new User(email, passwordEncoder.encode(request.password()), request.name());
        userRepository.save(user);
        return new SignupResponse(user.getId(), user.getEmail(), user.getName());
    }

    public IssuedTokens login(LoginRequest request) {
        User user = userRepository.findByEmail(EmailNormalizer.normalize(request.email()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        return issueTokens(user.getId(), user.getEmail());
    }

    public IssuedTokens refresh(String presentedRefreshToken) {
        var claims = jwtTokenProvider.parseClaims(presentedRefreshToken);
        if (claims == null) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        Long userId = jwtTokenProvider.getUserId(claims);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());
        String replacementRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
        if (!refreshTokenService.rotate(userId, presentedRefreshToken, replacementRefreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        return new IssuedTokens(new TokenResponse(accessToken, jwtTokenProvider.getAccessTokenExpiry().toSeconds()),
                replacementRefreshToken);
    }

    public void logout(Long userId) {
        refreshTokenService.delete(userId);
    }

    private IssuedTokens issueTokens(Long userId, String email) {
        String accessToken = jwtTokenProvider.generateAccessToken(userId, email);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userId);
        refreshTokenService.save(userId, refreshToken);
        TokenResponse body = new TokenResponse(accessToken, jwtTokenProvider.getAccessTokenExpiry().toSeconds());
        return new IssuedTokens(body, refreshToken);
    }

    /** Pairs the JSON response body with the refresh token that only ever goes into a Set-Cookie header. */
    public record IssuedTokens(TokenResponse body, String refreshToken) {
    }
}
