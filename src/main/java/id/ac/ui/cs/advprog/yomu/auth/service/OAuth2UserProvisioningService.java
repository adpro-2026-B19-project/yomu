package id.ac.ui.cs.advprog.yomu.auth.service;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.repository.AuthRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OAuth2UserProvisioningService {

    private final AuthRepository authRepository;
    private final UsernameUniquenessService usernameUniquenessService;

    public OAuth2UserProvisioningService(
            AuthRepository authRepository,
            UsernameUniquenessService usernameUniquenessService
    ) {
        this.authRepository = authRepository;
        this.usernameUniquenessService = usernameUniquenessService;
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

        String username = generateGeneratedUsername();
        String displayName = username;
        AuthUser user = new AuthUser(username, identity.email(), null, displayName, null);
        return authRepository.save(user);
    }

    private String generateGeneratedUsername() {
        for (int suffix = 1; suffix <= 9999; suffix++) {
            String candidate = "user" + suffix;
            if (!usernameUniquenessService.isUsernameTaken(candidate)) {
                return candidate;
            }
        }

        throw new IllegalStateException("Unable to generate a unique username for OAuth2 user");
    }
}
