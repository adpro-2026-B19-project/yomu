package id.ac.ui.cs.advprog.yomu.auth.service;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.model.PasswordStrength;
import id.ac.ui.cs.advprog.yomu.auth.repository.AuthRepository;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthRepository authRepository;
    private final EmailExistenceChecker emailExistenceChecker;
    private final PasswordStrengthChecker passwordStrengthChecker;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(
            AuthRepository authRepository,
            EmailExistenceChecker emailExistenceChecker,
            PasswordStrengthChecker passwordStrengthChecker,
            PasswordEncoder passwordEncoder
    ) {
        this.authRepository = authRepository;
        this.emailExistenceChecker = emailExistenceChecker;
        this.passwordStrengthChecker = passwordStrengthChecker;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public RegistrationResult registerUser(RegisterRequest request) {
        String normalizedEmail = normalize(request.email());
        String normalizedPassword = normalize(request.password());
        String normalizedUsername = normalize(request.username());

        RegistrationResult requiredFieldsResult = validateRequiredRegistrationFields(normalizedEmail, normalizedPassword);
        if (requiredFieldsResult != null) {
            return requiredFieldsResult;
        }

        RegistrationResult emailExistenceResult = validateEmailExistence(normalizedEmail);
        if (emailExistenceResult != null) {
            return emailExistenceResult;
        }

        PasswordStrength passwordStrength = passwordStrengthChecker.assess(normalizedPassword);
        RegistrationResult passwordStrengthResult = validatePasswordStrength(passwordStrength);
        if (passwordStrengthResult != null) {
            return passwordStrengthResult;
        }

        RegistrationResult usernamePresenceResult = validateUsernamePresence(normalizedUsername);
        if (usernamePresenceResult != null) {
            return usernamePresenceResult;
        }

        RegistrationResult uniquenessResult = validateUniqueCredentials(normalizedEmail, normalizedUsername);
        if (uniquenessResult != null) {
            return uniquenessResult;
        }

        String hashedPassword = passwordEncoder.encode(normalizedPassword);
        AuthUser user = new AuthUser(
                normalizedUsername,
                normalizedEmail,
                null,
                normalizedUsername,
                hashedPassword
        );
        authRepository.save(user);
        return RegistrationResult.successResult(
                new RegisteredUserSummary(user.getUsername(), user.getEmail()),
                passwordStrength
        );
    }

    private RegistrationResult validateRequiredRegistrationFields(String email, String password) {
        if (email.isBlank()) {
            return RegistrationResult.failureResult("required_email", "Email is required");
        }

        if (password.isBlank()) {
            return RegistrationResult.failureResult("required_password", "Password is required");
        }

        return null;
    }

    private RegistrationResult validateEmailExistence(String email) {
        if (!emailExistenceChecker.exists(email)) {
            return RegistrationResult.failureResult("nonexistent_email", "Email does not exist");
        }

        return null;
    }

    private RegistrationResult validatePasswordStrength(PasswordStrength passwordStrength) {
        if (passwordStrength == PasswordStrength.WEAK) {
            return RegistrationResult.failureResult("weak_password", "Password is too weak", passwordStrength);
        }

        return null;
    }

    private RegistrationResult validateUsernamePresence(String username) {
        if (username.isBlank()) {
            return RegistrationResult.failureResult("required_username", "Username is required");
        }

        return null;
    }

    private RegistrationResult validateUniqueCredentials(String email, String username) {
        if (authRepository.existsByEmail(email)) {
            return RegistrationResult.failureResult("duplicate_email", "Email is already registered");
        }

        if (authRepository.existsByUsername(username)) {
            return RegistrationResult.failureResult("duplicate_username", "Username is already taken");
        }

        return null;
    }

    @Override
    public LoginResult loginUser(LoginRequest request) {
        String normalizedEmail = normalize(request.email());
        String normalizedPassword = normalize(request.password());

        if (normalizedEmail.isBlank()) {
            return LoginResult.failureResult("required_email", "Email is required");
        }

        if (normalizedPassword.isBlank()) {
            return LoginResult.failureResult("required_password", "Password is required");
        }

        Optional<AuthUser> userOptional = authRepository.findByEmail(normalizedEmail);
        if (userOptional.isEmpty()) {
            return LoginResult.failureResult("invalid_credentials", "Invalid email or password");
        }

        AuthUser user = userOptional.get();
        String storedPassword = user.getPassword();
        if (storedPassword == null || storedPassword.isBlank()) {
            return LoginResult.failureResult("invalid_credentials", "Invalid email or password");
        }

        if (!passwordEncoder.matches(normalizedPassword, storedPassword)) {
            return LoginResult.failureResult("invalid_credentials", "Invalid email or password");
        }

        return LoginResult.successResult(new LoggedInUserSummary(user.getUsername(), user.getEmail()));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
