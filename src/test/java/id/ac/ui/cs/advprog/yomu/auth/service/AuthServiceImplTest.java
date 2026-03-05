package id.ac.ui.cs.advprog.yomu.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.model.PasswordStrength;
import id.ac.ui.cs.advprog.yomu.auth.repository.AuthRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Mock
    private AuthRepository authRepository;

    @Mock
    private EmailExistenceChecker emailExistenceChecker;

    @Mock
    private PasswordStrengthChecker passwordStrengthChecker;

    @Spy
    private PasswordEncoder mockedPasswordEncoder = new BCryptPasswordEncoder();

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        lenient().when(emailExistenceChecker.exists(anyString())).thenReturn(true);
        lenient().when(passwordStrengthChecker.assess(anyString())).thenReturn(PasswordStrength.STRONG);
    }

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
    void registerUserShouldFailWhenEmailAlreadyExists() {
        when(authRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(new AuthUser("alice", "alice@example.com", null, "alice", "hashed")));

        AuthService.RegistrationResult result = authService.registerUser(
                new AuthService.RegisterRequest("alice@example.com", "alice", "RawPassword1!")
        );

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo("duplicate_email");
        assertThat(result.errorMessage()).isEqualTo("Email already exists");
        verify(authRepository).findByEmail("alice@example.com");
        verify(authRepository, never()).save(any());
    }

    @Test
    void registerUserShouldFailWhenUsernameAlreadyExists() {
        when(authRepository.findByEmail("alice@example.com")).thenReturn(Optional.empty());
        when(authRepository.findByUsername("alice"))
                .thenReturn(Optional.of(new AuthUser("alice", "other@example.com", null, "alice", "hashed")));

        AuthService.RegistrationResult result = authService.registerUser(
                new AuthService.RegisterRequest("alice@example.com", "alice", "RawPassword1!")
        );

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo("duplicate_username");
        assertThat(result.errorMessage()).isEqualTo("Username already exists");
        verify(authRepository).findByEmail("alice@example.com");
        verify(authRepository).findByUsername("alice");
        verify(authRepository, never()).save(any());
    }

    @Test
    void registerUserShouldDefaultUsernameFromEmailLocalPart() {
        when(authRepository.findByEmail("charlie@example.com")).thenReturn(Optional.empty());
        when(authRepository.findByUsername("charlie")).thenReturn(Optional.empty());

        AuthService.RegistrationResult result = authService.registerUser(
                new AuthService.RegisterRequest("charlie@example.com", "   ", "RawPassword1!")
        );

        assertThat(result.success()).isTrue();
        assertThat(result.errorCode()).isNull();
        assertThat(result.errorMessage()).isNull();

        ArgumentCaptor<AuthUser> userCaptor = ArgumentCaptor.forClass(AuthUser.class);
        verify(authRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("charlie@example.com");
        assertThat(userCaptor.getValue().getUsername()).isEqualTo("charlie");
        assertThat(userCaptor.getValue().getDisplayName()).isEqualTo("charlie");
    }

    @Test
    void registerUserShouldPersistHashedPasswordWhenValid() {
        when(authRepository.findByEmail("dora@example.com")).thenReturn(Optional.empty());
        when(authRepository.findByUsername("dora")).thenReturn(Optional.empty());

        AuthService.RegistrationResult result = authService.registerUser(
                new AuthService.RegisterRequest("dora@example.com", "dora", "SecretPassword1!")
        );

        assertThat(result.success()).isTrue();

        ArgumentCaptor<AuthUser> userCaptor = ArgumentCaptor.forClass(AuthUser.class);
        verify(authRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isNotEqualTo("SecretPassword1!");
        assertThat(userCaptor.getValue().getPassword()).startsWith("$2");
    }

    @Test
    void registerUserShouldFailWhenEmailDoesNotExist() {
        when(emailExistenceChecker.exists("ghost@missing.invalid")).thenReturn(false);

        AuthService.RegistrationResult result = authService.registerUser(
                new AuthService.RegisterRequest("ghost@missing.invalid", "ghost", "GhostPass1!")
        );

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo("nonexistent_email");
        assertThat(result.errorMessage()).isEqualTo("Email does not exist");
        verify(authRepository, never()).save(any());
    }

    @Test
    void registerUserShouldFailWhenPasswordStrengthIsWeak() {
        when(passwordStrengthChecker.assess("weakpass")).thenReturn(PasswordStrength.WEAK);

        AuthService.RegistrationResult result = authService.registerUser(
                new AuthService.RegisterRequest("weak@example.com", "weak", "weakpass")
        );

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo("weak_password");
        assertThat(result.errorMessage()).isEqualTo("Password is too weak");
        assertThat(result.passwordStrength()).isEqualTo(PasswordStrength.WEAK);
        verify(authRepository, never()).save(any());
    }

    @Test
    void loginUserShouldFailWhenEmailIsBlank() {
        AuthService.LoginResult result = authService.loginUser(new AuthService.LoginRequest("   ", "SecretPass1!"));

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo("required_email");
        assertThat(result.errorMessage()).isEqualTo("Email is required");
    }

    @Test
    void loginUserShouldFailWhenPasswordIsBlank() {
        AuthService.LoginResult result = authService.loginUser(new AuthService.LoginRequest("alice@example.com", "   "));

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo("required_password");
        assertThat(result.errorMessage()).isEqualTo("Password is required");
    }

    @Test
    void loginUserShouldFailWhenEmailIsNotRegistered() {
        when(authRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        AuthService.LoginResult result = authService.loginUser(new AuthService.LoginRequest("ghost@example.com", "SecretPass1!"));

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo("invalid_credentials");
        assertThat(result.errorMessage()).isEqualTo("Invalid email or password");
    }

    @Test
    void loginUserShouldFailWhenPasswordDoesNotMatch() {
        String hashedPassword = passwordEncoder.encode("CorrectPass1!");
        when(authRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(new AuthUser("alice", "alice@example.com", null, "alice", hashedPassword)));

        AuthService.LoginResult result = authService.loginUser(new AuthService.LoginRequest("alice@example.com", "WrongPass1!"));

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo("invalid_credentials");
        assertThat(result.errorMessage()).isEqualTo("Invalid email or password");
    }

    @Test
    void loginUserShouldSucceedWhenCredentialsAreValid() {
        String hashedPassword = passwordEncoder.encode("CorrectPass1!");
        when(authRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(new AuthUser("alice", "alice@example.com", null, "alice", hashedPassword)));

        AuthService.LoginResult result = authService.loginUser(new AuthService.LoginRequest("alice@example.com", "CorrectPass1!"));

        assertThat(result.success()).isTrue();
        assertThat(result.errorCode()).isNull();
        assertThat(result.errorMessage()).isNull();
        assertThat(result.loggedInUser()).isNotNull();
        assertThat(result.loggedInUser().username()).isEqualTo("alice");
        assertThat(result.loggedInUser().email()).isEqualTo("alice@example.com");
    }
}
