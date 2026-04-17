package id.ac.ui.cs.advprog.yomu.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthRole;
import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.repository.AuthRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    @Mock
    private AuthRepository authRepository;

    @InjectMocks
    private ProfileServiceImpl profileService;

    @Test
    void updateProfileShouldUpdateAllowedFieldsWhenUsernameUnique() {
        UUID userId = UUID.randomUUID();
        AuthUser user = new AuthUser("alice", "alice@example.com", 628111111111L, "Alice", "hashed");
        ReflectionTestUtils.setField(user, "id", userId);

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(authRepository.existsByUsername("alice-updated")).thenReturn(false);

        ProfileService.UpdateProfileResult result = profileService.updateProfile(
                new ProfileService.UpdateProfileRequest(userId, "alice-updated", "Alice Updated", 628222222222L)
        );

        assertThat(result.success()).isTrue();
        assertThat(result.errorCode()).isNull();
        assertThat(result.updatedProfile()).isNotNull();
        assertThat(result.updatedProfile().username()).isEqualTo("alice-updated");
        assertThat(result.updatedProfile().displayName()).isEqualTo("Alice Updated");
        assertThat(result.updatedProfile().phoneNumber()).isEqualTo(628222222222L);

        ArgumentCaptor<AuthUser> userCaptor = ArgumentCaptor.forClass(AuthUser.class);
        verify(authRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getUsername()).isEqualTo("alice-updated");
        assertThat(userCaptor.getValue().getDisplayName()).isEqualTo("Alice Updated");
        assertThat(userCaptor.getValue().getPhoneNumber()).isEqualTo(628222222222L);
    }

    @Test
    void updateProfileShouldFailWhenRequestedUsernameAlreadyExists() {
        UUID userId = UUID.randomUUID();
        AuthUser user = new AuthUser("alice", "alice@example.com", null, "Alice", "hashed");
        ReflectionTestUtils.setField(user, "id", userId);

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(authRepository.existsByUsername("taken-name")).thenReturn(true);

        ProfileService.UpdateProfileResult result = profileService.updateProfile(
                new ProfileService.UpdateProfileRequest(userId, "taken-name", "Alice Updated", 628111111111L)
        );

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo("duplicate_username");
        assertThat(result.errorMessage()).isEqualTo("Username is already taken");
        verify(authRepository, never()).save(any());
    }

    @Test
    void updateProfileShouldNotFailWhenUsernameUnchanged() {
        UUID userId = UUID.randomUUID();
        AuthUser user = new AuthUser("alice", "alice@example.com", null, "Alice", "hashed");
        ReflectionTestUtils.setField(user, "id", userId);

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));

        ProfileService.UpdateProfileResult result = profileService.updateProfile(
                new ProfileService.UpdateProfileRequest(userId, "alice", "Alice Updated", 628111111111L)
        );

        assertThat(result.success()).isTrue();
        verify(authRepository, never()).existsByUsername(anyString());
        verify(authRepository).save(user);
    }

    @Test
    void updateProfileShouldNotOverwriteUnrelatedFields() {
        UUID userId = UUID.randomUUID();
        AuthUser user = new AuthUser("alice", "alice@example.com", 628111111111L, "Alice", "super-secret", AuthRole.ADMIN);
        ReflectionTestUtils.setField(user, "id", userId);
        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 17, 12, 0);
        ReflectionTestUtils.setField(user, "createdAt", createdAt);

        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(authRepository.existsByUsername("alice-updated")).thenReturn(false);

        ProfileService.UpdateProfileResult result = profileService.updateProfile(
                new ProfileService.UpdateProfileRequest(userId, "alice-updated", "Alice Updated", 628222222222L)
        );

        assertThat(result.success()).isTrue();
        assertThat(user.getEmail()).isEqualTo("alice@example.com");
        assertThat(user.getPassword()).isEqualTo("super-secret");
        assertThat(user.getRole()).isEqualTo(AuthRole.ADMIN);
        assertThat(user.getCreatedAt()).isEqualTo(createdAt);
    }
}
