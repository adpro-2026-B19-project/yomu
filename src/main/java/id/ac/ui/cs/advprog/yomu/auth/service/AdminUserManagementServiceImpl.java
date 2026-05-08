package id.ac.ui.cs.advprog.yomu.auth.service;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthRole;
import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.repository.AuthRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserManagementServiceImpl implements AdminUserManagementService {

    private final AuthRepository authRepository;

    public AdminUserManagementServiceImpl(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuthUser> searchUsers(String keyword, AuthRole role, Boolean active, Pageable pageable) {
        String normalizedKeyword = normalize(keyword);
        return authRepository.searchUsers(normalizedKeyword, role, active, pageable);
    }

    @Override
    @Transactional
    public boolean updateUserStatus(UUID userId, boolean active) {
        return authRepository.findById(userId).map(user -> {
            if (active) {
                user.activate();
            } else {
                user.deactivate();
            }
            return true;
        }).orElse(false);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

