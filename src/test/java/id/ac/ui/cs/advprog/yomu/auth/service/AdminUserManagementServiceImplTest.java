package id.ac.ui.cs.advprog.yomu.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthRole;
import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.repository.AuthRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class AdminUserManagementServiceImplTest {

    @Mock
    private AuthRepository authRepository;

    @Test
    void searchUsersShouldNormalizeBlankKeywordToNullAndTrimNonBlankKeyword() {
        AdminUserManagementServiceImpl service = new AdminUserManagementServiceImpl(authRepository);
        PageRequest pageable = PageRequest.of(0, 10);

        service.searchUsers(null, null, null, pageable);
        service.searchUsers("   ", AuthRole.USER, true, pageable);
        service.searchUsers("  reader  ", AuthRole.ADMIN, false, pageable);

        verify(authRepository).searchUsers(null, null, null, pageable);
        verify(authRepository).searchUsers(null, AuthRole.USER, true, pageable);
        verify(authRepository).searchUsers("reader", AuthRole.ADMIN, false, pageable);
    }

    @Test
    void updateUserStatusShouldActivateDeactivateOrReturnFalseWhenMissing() {
        UUID activeId = UUID.randomUUID();
        AuthUser activeUser = new AuthUser("active", "active@example.com", null, "Active", "hash");
        activeUser.deactivate();
        UUID inactiveId = UUID.randomUUID();
        AuthUser inactiveUser = new AuthUser("inactive", "inactive@example.com", null, "Inactive", "hash");
        UUID missingId = UUID.randomUUID();
        when(authRepository.findById(activeId)).thenReturn(Optional.of(activeUser));
        when(authRepository.findById(inactiveId)).thenReturn(Optional.of(inactiveUser));
        when(authRepository.findById(missingId)).thenReturn(Optional.empty());
        AdminUserManagementServiceImpl service = new AdminUserManagementServiceImpl(authRepository);

        assertThat(service.updateUserStatus(activeId, true)).isTrue();
        assertThat(activeUser.isActive()).isTrue();
        assertThat(activeUser.getDeletedAt()).isNull();

        assertThat(service.updateUserStatus(inactiveId, false)).isTrue();
        assertThat(inactiveUser.isActive()).isFalse();
        assertThat(inactiveUser.getDeletedAt()).isNotNull();

        assertThat(service.updateUserStatus(missingId, true)).isFalse();
    }
}
