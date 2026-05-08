package id.ac.ui.cs.advprog.yomu.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.repository.AuthRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OAuth2UserProvisioningServiceTest {

    @Mock
    private AuthRepository authRepository;

    @Mock
    private UsernameUniquenessService usernameUniquenessService;

    @Spy
    private AuthIdentifierValidator authIdentifierValidator = new AuthIdentifierValidator();

    @InjectMocks
    private OAuth2UserProvisioningService provisioningService;

    @Test
    void loadOrCreateUserShouldReturnExistingUserWhenEmailAlreadyRegistered() {
        AuthUser existing = new AuthUser("reader", "reader@example.com", null, "Reader", null);
        when(authRepository.findByEmailAndActiveTrue("reader@example.com")).thenReturn(Optional.of(existing));

        AuthUser resolved = provisioningService.loadOrCreateUser(
                new OAuth2UserIdentity("google", "reader@example.com", "reader", "Reader")
        );

        assertThat(resolved).isSameAs(existing);
        verify(authRepository, never()).save(any());
    }

    @Test
    void loadOrCreateUserShouldCreateUserWhenEmailIsNew() {
        when(authRepository.findByEmailAndActiveTrue("new@example.com")).thenReturn(Optional.empty());
        when(authRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(usernameUniquenessService.isUsernameTaken("new-user")).thenReturn(false);
        when(authRepository.save(any(AuthUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthUser created = provisioningService.loadOrCreateUser(
                new OAuth2UserIdentity("github", "new@example.com", "new user", "New User")
        );

        assertThat(created.getEmail()).isEqualTo("new@example.com");
        assertThat(created.getUsername()).isEqualTo("new-user");
        assertThat(created.getDisplayName()).isEqualTo("New User");
        assertThat(created.getPassword()).isNull();
    }

    @Test
    void loadOrCreateUserShouldAppendSuffixWhenUsernameAlreadyTaken() {
        when(authRepository.findByEmailAndActiveTrue("reader@example.com")).thenReturn(Optional.empty());
        when(authRepository.findByEmail("reader@example.com")).thenReturn(Optional.empty());
        when(usernameUniquenessService.isUsernameTaken("reader")).thenReturn(true);
        when(usernameUniquenessService.isUsernameTaken("reader2")).thenReturn(false);
        when(authRepository.save(any(AuthUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        provisioningService.loadOrCreateUser(
                new OAuth2UserIdentity("google", "reader@example.com", "reader", "Reader Name")
        );

        ArgumentCaptor<AuthUser> captor = ArgumentCaptor.forClass(AuthUser.class);
        verify(authRepository).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("reader2");
    }
}
