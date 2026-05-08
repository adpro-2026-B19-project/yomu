package id.ac.ui.cs.advprog.yomu.auth.service;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthRole;
import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUserManagementService {

    Page<AuthUser> searchUsers(String keyword, AuthRole role, Boolean active, Pageable pageable);

    boolean updateUserStatus(java.util.UUID userId, boolean active);
}

