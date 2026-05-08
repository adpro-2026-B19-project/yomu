package id.ac.ui.cs.advprog.yomu.auth.service;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.model.AuthRole;
import id.ac.ui.cs.advprog.yomu.auth.repository.AuthRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class ProfileServiceImpl implements ProfileService {

    private final AuthRepository authRepository;
    private final UsernameUniquenessService usernameUniquenessService;
    private final PasswordEncoder passwordEncoder;

    public ProfileServiceImpl(
            AuthRepository authRepository,
            UsernameUniquenessService usernameUniquenessService,
            PasswordEncoder passwordEncoder
    ) {
        this.authRepository = authRepository;
        this.usernameUniquenessService = usernameUniquenessService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UpdateProfileResult updateProfile(UpdateProfileRequest request) {
        AuthUser user = resolveUser(request.userId());
        if (user == null) {
            return UpdateProfileResult.failureResult("user_not_found", "User not found");
        }

        String normalizedUsername = normalize(request.username());
        UpdateProfileResult usernameValidationResult = validateRequestedUsername(normalizedUsername, user.getUsername());
        if (usernameValidationResult != null) {
            return usernameValidationResult;
        }

        String updatedDisplayName = resolveDisplayName(user.getDisplayName(), request.displayName());

        applyProfileChanges(user, normalizedUsername, updatedDisplayName, request.phoneNumber());

        return buildSuccessResult(user);
    }

    @Override
    public DeleteAccountResult deleteOwnAccount(DeleteAccountRequest request) {
        AuthUser user = resolveUser(request.userId());
        if (user == null || !user.isActive()) {
            return DeleteAccountResult.failureResult("user_not_found", "Account could not be deleted");
        }

        if (user.getRole() != AuthRole.USER) {
            return DeleteAccountResult.failureResult("forbidden", "Account could not be deleted");
        }

        String normalizedPassword = normalize(request.password());
        String storedPassword = user.getPassword();
        if (normalizedPassword.isBlank() || storedPassword == null || storedPassword.isBlank()) {
            return DeleteAccountResult.failureResult("invalid_credentials", "Invalid credentials");
        }

        if (!passwordEncoder.matches(normalizedPassword, storedPassword)) {
            return DeleteAccountResult.failureResult("invalid_credentials", "Invalid credentials");
        }

        user.deactivate();
        authRepository.save(user);
        return DeleteAccountResult.successResult();
    }

    private AuthUser resolveUser(UUID userId) {
        if (userId == null) {
            return null;
        }
        Optional<AuthUser> userOptional = authRepository.findById(userId);
        return userOptional.orElse(null);
    }

    private UpdateProfileResult validateRequestedUsername(String requestedUsername, String currentUsername) {
        if (requestedUsername.isBlank()) {
            return UpdateProfileResult.failureResult("required_username", "Username is required");
        }

        if (usernameUniquenessService.isUsernameTakenByAnotherUser(requestedUsername, currentUsername)) {
            return UpdateProfileResult.failureResult("duplicate_username", "Username is already taken");
        }

        return null;
    }

    private String resolveDisplayName(String currentDisplayName, String requestedDisplayName) {
        String normalizedDisplayName = normalize(requestedDisplayName);
        return normalizedDisplayName.isBlank() ? currentDisplayName : normalizedDisplayName;
    }

    private void applyProfileChanges(AuthUser user, String username, String displayName, Long phoneNumber) {
        user.updateProfile(username, displayName, phoneNumber);
        authRepository.save(user);
    }

    private UpdateProfileResult buildSuccessResult(AuthUser user) {
        return UpdateProfileResult.successResult(
                new UpdatedProfileSummary(user.getUsername(), user.getEmail(), user.getDisplayName(), user.getPhoneNumber())
        );
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
