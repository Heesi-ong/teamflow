package com.teamflow.user;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Read-only entry point other modules use instead of reaching into
 * UserRepository/User directly, per 05-backend-architecture.md §3
 * ("다른 Module의 내부 구현에 직접 접근하지 않고 Service를 통해서만 협력").
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Map<Long, UserSummary> getSummaries(Collection<Long> userIds) {
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, UserSummary::from, (a, b) -> a));
    }

    public UserSummary getSummary(Long userId) {
        return userRepository.findById(userId).map(UserSummary::from).orElse(null);
    }

    public Optional<Long> findUserIdByEmail(String email) {
        return userRepository.findByEmail(email).map(User::getId);
    }
}
