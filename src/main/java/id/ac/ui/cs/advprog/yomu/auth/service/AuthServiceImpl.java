package id.ac.ui.cs.advprog.yomu.auth.service;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.repository.AuthRepository;
import java.util.List;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthRepository authRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthServiceImpl(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    @Override
    public List<AuthUser> findAllUsers() {
        return authRepository.findAllByOrderByIdDesc();
    }

    @Override
    public long countUsers() {
        return authRepository.count();
    }

    @Override
    public RegistrationResult registerUser(RegisterRequest request) {
        String normalizedEmail = normalize(request.email());
        String normalizedPassword = normalize(request.password());

        if (normalizedEmail.isBlank()) {
            return RegistrationResult.failureResult("required_email", "Email is required");
        }

        if (normalizedPassword.isBlank()) {
            return RegistrationResult.failureResult("required_password", "Password is required");
        }

        String normalizedUsername = normalize(request.username());
        if (normalizedUsername.isBlank()) {
            normalizedUsername = deriveUsernameFromEmail(normalizedEmail);
        }

        if (normalizedUsername.isBlank()) {
            return RegistrationResult.failureResult("required_username", "Username is required");
        }

        if (authRepository.findByEmail(normalizedEmail).isPresent()) {
            return RegistrationResult.failureResult("duplicate_email", "Email already exists");
        }

        if (authRepository.findByUsername(normalizedUsername).isPresent()) {
            return RegistrationResult.failureResult("duplicate_username", "Username already exists");
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
        return RegistrationResult.successResult();
    }

    private String deriveUsernameFromEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "";
        }
        return email.substring(0, atIndex).trim();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
