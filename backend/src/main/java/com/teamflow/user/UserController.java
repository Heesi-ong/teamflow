package com.teamflow.user;

import com.teamflow.auth.UserPrincipal;
import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorCode;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 08-api-specification.md §1-1. */
@RestController
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/api/users/me")
    public UserResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        return UserResponse.from(user);
    }
}
