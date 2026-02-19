package id.ac.ui.cs.advprog.yomu.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.repository.AuthRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthRepository authRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void findAllUsersShouldDelegateToRepository() {
        List<AuthUser> expectedUsers = List.of(new AuthUser("alice"), new AuthUser("bob"));
        when(authRepository.findAllByOrderByIdDesc()).thenReturn(expectedUsers);

        List<AuthUser> result = authService.findAllUsers();

        assertThat(result).isEqualTo(expectedUsers);
        verify(authRepository).findAllByOrderByIdDesc();
    }

    @Test
    void countUsersShouldDelegateToRepository() {
        when(authRepository.count()).thenReturn(5L);

        long count = authService.countUsers();

        assertThat(count).isEqualTo(5L);
        verify(authRepository).count();
    }

    @Test
    void registerUserShouldFailWhenUsernameIsBlankAfterTrim() {
        AuthService.RegistrationResult result = authService.registerUser("   ");

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo("required");
        assertThat(result.errorMessage()).isEqualTo("Username is required");
        verify(authRepository, never()).findByUsername(any());
        verify(authRepository, never()).save(any());
    }

    @Test
    void registerUserShouldFailWhenUsernameAlreadyExists() {
        when(authRepository.findByUsername("alice")).thenReturn(Optional.of(new AuthUser("alice")));

        AuthService.RegistrationResult result = authService.registerUser(" alice ");

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo("duplicate");
        assertThat(result.errorMessage()).isEqualTo("Username already exists");
        verify(authRepository).findByUsername("alice");
        verify(authRepository, never()).save(any());
    }

    @Test
    void registerUserShouldPersistNormalizedUsernameWhenValid() {
        when(authRepository.findByUsername("charlie")).thenReturn(Optional.empty());

        AuthService.RegistrationResult result = authService.registerUser(" charlie ");

        assertThat(result.success()).isTrue();
        assertThat(result.errorCode()).isNull();
        assertThat(result.errorMessage()).isNull();

        ArgumentCaptor<AuthUser> userCaptor = ArgumentCaptor.forClass(AuthUser.class);
        verify(authRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getUsername()).isEqualTo("charlie");
    }
}
