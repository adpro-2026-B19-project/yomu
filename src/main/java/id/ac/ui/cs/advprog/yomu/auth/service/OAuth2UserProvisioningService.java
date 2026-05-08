package id.ac.ui.cs.advprog.yomu.auth.service;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.repository.AuthRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OAuth2UserProvisioningService {

    private static final int MAX_USERNAME_LENGTH = 40;

    private final AuthRepository authRepository;
    private final UsernameUniquenessService usernameUniquenessService;
    private final AuthIdentifierValidator authIdentifierValidator;

    public OAuth2UserProvisioningService(
            AuthRepository authRepository,
            UsernameUniquenessService usernameUniquenessService,
            AuthIdentifierValidator authIdentifierValidator
    ) {
        this.authRepository = authRepository;
        this.usernameUniquenessService = usernameUniquenessService;
        this.authIdentifierValidator = authIdentifierValidator;
    }

    @Transactional
    public AuthUser loadOrCreateUser(OAuth2UserIdentity identity) {
        Optional<AuthUser> existingUser = authRepository.findByEmailAndActiveTrue(identity.email());
        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        if (authRepository.findByEmail(identity.email()).isPresent()) {
            throw new IllegalStateException("OAuth2 account is deactivated");
        }

        String username = generateUniqueUsername(identity.preferredUsername(), identity.email());
        String displayName = resolveDisplayName(identity.displayName(), username);
        AuthUser user = new AuthUser(username, identity.email(), null, displayName, null);
        return authRepository.save(user);
    }

    private String generateUniqueUsername(String preferredUsername, String email) {
        String fallback = emailLocalPart(email);
        String base = normalizeUsername(firstNonBlank(preferredUsername, fallback, "user"));
        if (base.isBlank()) {
            base = "user";
        }
        if (base.length() < 3) {
            base = (base + "___").substring(0, 3);
        }

        String candidate = trimToMaxLength(base, MAX_USERNAME_LENGTH);
        if (!usernameUniquenessService.isUsernameTaken(candidate)) {
            return candidate;
        }

        int suffix = 2;
        while (suffix <= 9999) {
            String suffixText = String.valueOf(suffix);
            String withSuffix = trimToMaxLength(base, MAX_USERNAME_LENGTH - suffixText.length()) + suffixText;
            if (!usernameUniquenessService.isUsernameTaken(withSuffix)) {
                return withSuffix;
            }
            suffix++;
        }

        throw new IllegalStateException("Unable to generate a unique username for OAuth2 user");
    }

    private String resolveDisplayName(String displayName, String username) {
        String normalizedDisplayName = normalize(displayName);
        return normalizedDisplayName.isBlank() ? username : normalizedDisplayName;
    }

    private String normalizeUsername(String username) {
        String sanitized = normalize(username)
                .toLowerCase()
                .replaceAll("[^A-Za-z0-9._-]", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^[-._]+|[-._]+$", "");

        if (authIdentifierValidator.isValidUsername(sanitized)) {
            return sanitized;
        }

        String compacted = sanitized.replaceAll("[^A-Za-z0-9]", "");
        if (authIdentifierValidator.isValidUsername(compacted)) {
            return compacted;
        }
        return sanitized;
    }

    private String emailLocalPart(String email) {
        String normalizedEmail = normalize(email);
        int atIndex = normalizedEmail.indexOf('@');
        if (atIndex <= 0) {
            return "";
        }
        return normalizedEmail.substring(0, atIndex);
    }

    private String trimToMaxLength(String value, int maxLength) {
        if (maxLength <= 0) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            String normalized = normalize(candidate);
            if (!normalized.isBlank()) {
                return normalized;
            }
        }
        return "";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
