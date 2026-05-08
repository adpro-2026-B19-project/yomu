package id.ac.ui.cs.advprog.yomu.auth.service;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.repository.AuthRepository;
import java.util.Optional;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserResolverImpl implements CurrentUserResolver {

    private final AuthRepository authRepository;

    public CurrentUserResolverImpl(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    @Override
    public Optional<AuthUser> resolveUser(Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthPrincipalIdentity principalIdentity) {
            return authRepository.findByEmailAndActiveTrue(principalIdentity.getEmail())
                    .or(() -> authRepository.findByUsernameAndActiveTrue(principalIdentity.getUsername()));
        }
        return authRepository.findByUsernameAndActiveTrue(authentication.getName());
    }

    @Override
    public Optional<String> resolveUsername(Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthPrincipalIdentity principalIdentity) {
            return Optional.of(principalIdentity.getUsername());
        }
        return Optional.ofNullable(authentication.getName());
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
