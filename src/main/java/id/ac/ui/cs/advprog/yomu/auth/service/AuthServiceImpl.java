package id.ac.ui.cs.advprog.yomu.auth.service;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.repository.AuthRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthRepository authRepository;

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
    public RegistrationResult registerUser(String username) {
        String normalizedUsername = username == null ? "" : username.trim();

        if (normalizedUsername.isBlank()) {
            return RegistrationResult.failureResult("required", "Username is required");
        }

        if (authRepository.findByUsername(normalizedUsername).isPresent()) {
            return RegistrationResult.failureResult("duplicate", "Username already exists");
        }

        authRepository.save(new AuthUser(normalizedUsername));
        return RegistrationResult.successResult();
    }
}
