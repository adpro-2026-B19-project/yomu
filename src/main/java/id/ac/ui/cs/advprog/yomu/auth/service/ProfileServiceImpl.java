package id.ac.ui.cs.advprog.yomu.auth.service;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.repository.AuthRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ProfileServiceImpl implements ProfileService {

    private final AuthRepository authRepository;

    public ProfileServiceImpl(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    @Override
    public UpdateProfileResult updateProfile(UpdateProfileRequest request) {
        AuthUser user = resolveUser(request.userId());
        if (user == null) {
            return UpdateProfileResult.failureResult("user_not_found", "User not found");
        }

        String normalizedUsername = normalize(request.username());
        if (normalizedUsername.isBlank()) {
            return UpdateProfileResult.failureResult("required_username", "Username is required");
        }

        if (isUsernameTakenByOtherUser(normalizedUsername, user.getUsername())) {
            return UpdateProfileResult.failureResult("duplicate_username", "Username is already taken");
        }

        String normalizedDisplayName = normalize(request.displayName());
        String updatedDisplayName = normalizedDisplayName.isBlank() ? user.getDisplayName() : normalizedDisplayName;

        user.updateProfile(normalizedUsername, updatedDisplayName, request.phoneNumber());
        authRepository.save(user);

        return UpdateProfileResult.successResult(
                new UpdatedProfileSummary(user.getUsername(), user.getEmail(), user.getDisplayName(), user.getPhoneNumber())
        );
    }

    private AuthUser resolveUser(UUID userId) {
        if (userId == null) {
            return null;
        }
        Optional<AuthUser> userOptional = authRepository.findById(userId);
        return userOptional.orElse(null);
    }

    private boolean isUsernameTakenByOtherUser(String requestedUsername, String currentUsername) {
        return !requestedUsername.equals(currentUsername) && authRepository.existsByUsername(requestedUsername);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
