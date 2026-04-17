package id.ac.ui.cs.advprog.yomu.auth.service;

import id.ac.ui.cs.advprog.yomu.auth.repository.AuthRepository;
import org.springframework.stereotype.Service;

@Service
public class UsernameUniquenessService {

    private final AuthRepository authRepository;

    public UsernameUniquenessService(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public boolean isUsernameTaken(String username) {
        return authRepository.existsByUsername(username);
    }

    public boolean isUsernameTakenByAnotherUser(String requestedUsername, String currentUsername) {
        return !requestedUsername.equals(currentUsername) && isUsernameTaken(requestedUsername);
    }
}
